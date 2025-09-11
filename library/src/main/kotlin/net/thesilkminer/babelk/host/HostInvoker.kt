@file:JvmName("HostInvoker")

package net.thesilkminer.babelk.host

import net.thesilkminer.babelk.api.grammar.GrammarRule
import net.thesilkminer.babelk.api.invoke.InvocationConfiguration
import net.thesilkminer.babelk.api.invoke.StackOverflowCallback
import net.thesilkminer.babelk.script.api.grammar.NamedRule
import net.thesilkminer.babelk.script.api.grammar.Rule
import net.thesilkminer.babelk.script.api.invoke.AppendLiteral
import net.thesilkminer.babelk.script.api.invoke.ArgumentName
import net.thesilkminer.babelk.script.api.invoke.BuildingContext
import net.thesilkminer.babelk.script.api.invoke.BuildingEvent
import net.thesilkminer.babelk.script.api.invoke.BuildingEventList
import net.thesilkminer.babelk.script.api.invoke.CaptureInvocationMode
import net.thesilkminer.babelk.script.api.invoke.CaptureMode
import net.thesilkminer.babelk.script.api.invoke.CaptureStackOverflowMode
import net.thesilkminer.babelk.script.api.invoke.InvocationArguments
import net.thesilkminer.babelk.script.api.invoke.InvokableRule
import net.thesilkminer.babelk.script.api.invoke.PostInvokeSubRule
import net.thesilkminer.babelk.script.api.invoke.PreInvokeSubRule
import net.thesilkminer.babelk.script.api.invoke.RaiseStackOverflow
import net.thesilkminer.babelk.script.api.invoke.RandomSource
import net.thesilkminer.babelk.script.api.invoke.RuleState
import net.thesilkminer.babelk.script.api.invoke.RuleStateKey
import net.thesilkminer.babelk.script.api.provider.Provider
import kotlin.reflect.cast

private class InvokableRuleState(private val owningRule: Rule) : RuleState {
    private val states = mutableMapOf<String, Any>() // TODO("Maybe use RuleStateKey directly?")

    override val ruleName: String? get() = (this.owningRule as? NamedRule)?.name

    override fun <T : Any> get(key: RuleStateKey<T>): T? {
        return key.castMaybeNull(this.states[key.id])
    }

    override fun <T : Any> set(key: RuleStateKey<T>, value: T) {
        this.states[key.id] = value
    }

    override fun <T : Any> getOrPut(key: RuleStateKey<T>, default: () -> T): T {
        return key.castNotNull(this.states.getOrPut(key.id, default))
    }

    override fun <T : Any> update(key: RuleStateKey<T>, updater: (T?) -> T): T {
        return key.castNotNull(this.states.compute(key.id) { _, oldValue -> updater(key.castMaybeNull(oldValue)) }!!)
    }

    override fun <T : Any> updateIfPresent(key: RuleStateKey<T>, updater: (T) -> T): T? {
        return key.castMaybeNull(this.states.computeIfPresent(key.id) { _, oldValue -> updater(key.castNotNull(oldValue)) })
    }

    override fun toString(): String = "State{${this.ruleName ?: "<anonymous>"}}[${this.states}]"

    private fun <T : Any> RuleStateKey<T>.castNotNull(any: Any): T = this.type.cast(any)
    private fun <T : Any> RuleStateKey<T>.castMaybeNull(any: Any?): T? = any?.let { this.castNotNull(it) }
}

private class RuleStatesHolder(private val backend: MutableMap<Rule, InvokableRuleState> = mutableMapOf()) {
    operator fun get(rule: Rule): RuleState = this.backend.getOrPut(rule) { InvokableRuleState(rule) }
    operator fun get(provider: Provider<Rule>): RuleState = this[provider.get()]
    operator fun get(invokableRule: InvokableRule): RuleState = this[invokableRule.rule]
    override fun toString(): String = "States[${this.backend}]"
}

private typealias InvocationStackOverflowCallback = (error: StackOverflowError?) -> CharSequence?

private sealed class ScriptInvocationBuildingContext<B>(
    protected val rng: RandomSource,
    protected val remainingStackDepth: Int,
    protected val onStackOverflowCallback: InvocationStackOverflowCallback,
    protected val ruleStatesHolder: RuleStatesHolder,
    protected val builder: B
) : BuildingContext {
    abstract override fun append(literal: String)

    final override fun invoke(rule: InvokableRule) {
        // First verify if there is enough stack space for us to expand the target rule
        // We use 0 to indicate that the current invocation is the topmost on the stack, and we cannot add anything
        // newer, therefore any negative value indicates that the stack has been exhausted
        val newStackDepth = this.remainingStackDepth - 1
        if (newStackDepth < 0) {
            this.handleStackOverflow(null)
            return
        }

        // We have enough stack space to expand the current rule
        // Create a subcontext that shares the same rng, callback, and state holder. The stack depth changes, for
        // obvious reasons, but also incidentally we want to create a new builder. This means that if this invocation
        // (or one of its sub-invocation fails), then we don't append garbage data to the actual builder.
        // I wonder if this behavior makes sense but we can always change it during testing if need be, or put some
        // further configuration option in the InvocationConfiguration
        val subContext = this.createInvocationSubContext(newStackDepth)
        val ruleState = this.ruleStatesHolder[rule]

        // Invoke the rule catching any StackOverflowError. It's worth noting the following:
        // - if handling the stack overflow causes another stack overflow, then we assume we are quite deep into rule
        //   building and the SOE will get caught further down the stack until we have enough space to invoke the
        //   callback
        // - since the callback is always the same, if it is configured to rethrow the SOE immediately, then we will
        //   keep catching and rethrowing this exception; this is a huge performance sink, but this tradeoff is fine
        //   because we assume the situation to be quite rare (indeed the default is to use another exception)
        try {
            rule.onPreInvoke(subContext, ruleState)
            try {
                rule.invokeRule(subContext, ruleState)
            } catch (error: StackOverflowError) {
                this.handleStackOverflow(error)
                return
            }
        } finally {
            rule.onPostInvoke(subContext, ruleState)
        }

        // Invocation completed successfully, with subContext's StringBuilder having stored the output. We can complete
        // invocation by simply copying the data across the builders
        subContext.appendTo(this)
    }

    final override fun capture(mode: CaptureMode, block: (context: BuildingContext) -> Unit): BuildingEventList {
        // First we create a new context which will serve as our capturing context.
        // We can always do this because we want to support re-entering captures (so capturing while within a
        // capturing context)
        val subContext = this.createCapturingSubContext(mode)

        // Now we can invoke the block with the new context. This will populate the event list as needed. We do not
        // need to catch anything as it'll be handled by invocation and friends. Granted: we could end up with a SOE
        // if we have a context that keeps capturing in its captured block; we do not want to consider this case.
        block(subContext)

        // Finally we can extract the captured events and pass them over to the caller for processing. We call toList
        // in order to create a copy, so that the data cannot be modified down the line accidentally.
        return subContext.builder.toList()
    }

    protected abstract fun createInvocationSubContext(newStack: Int): ScriptInvocationBuildingContext<B>
    protected abstract fun createCapturingSubContext(mode: CaptureMode): ScriptInvocationBuildingContext<out BuildingEventList>

    protected abstract fun InvokableRule.onPreInvoke(context: ScriptInvocationBuildingContext<B>, state: RuleState)
    protected abstract fun InvokableRule.invokeRule(context: ScriptInvocationBuildingContext<B>, state: RuleState)
    protected abstract fun InvokableRule.onPostInvoke(context: ScriptInvocationBuildingContext<B>, state: RuleState)

    protected abstract fun handleStackOverflow(error: StackOverflowError?)
    protected abstract fun appendTo(parent: ScriptInvocationBuildingContext<B>)

    // Just there to bypass protected visibility restrictions
    protected inline fun appendToParentBuilder(parent: ScriptInvocationBuildingContext<B>, block: (B) -> Unit) {
        block(parent.builder)
    }

    protected operator fun InvokableRule.invoke(newContext: ScriptInvocationBuildingContext<B>, ruleState: RuleState) {
        this.rule.get().append(newContext, ruleState, newContext.rng, this.arguments)
    }

    final override fun toString(): String =
        "ScriptInvocationBuildingContext[rng=${this.rng},remainingStackDepth=${this.remainingStackDepth},onStackOverflowCallback=${this.onStackOverflowCallback}," +
            "builder=${this.builderToString(this.builder::toString)},ruleStatesHolder=${this.ruleStatesHolder}]"

    protected abstract fun builderToString(default: () -> String): String
}

private class RegularScriptInvocationBuildingContext(
    rng: RandomSource,
    remainingStackDepth: Int,
    onStackOverflowCallback: InvocationStackOverflowCallback,
    ruleStatesHolder: RuleStatesHolder,
    builder: StringBuilder = StringBuilder()
) : ScriptInvocationBuildingContext<StringBuilder>(rng, remainingStackDepth, onStackOverflowCallback, ruleStatesHolder, builder) {
    override fun append(literal: String) {
        this.append(literal as CharSequence)
    }

    override fun createInvocationSubContext(newStack: Int): ScriptInvocationBuildingContext<StringBuilder> {
        return RegularScriptInvocationBuildingContext(
            this.rng,
            newStack,
            this.onStackOverflowCallback,
            this.ruleStatesHolder
        )
    }

    override fun createCapturingSubContext(mode: CaptureMode): ScriptInvocationBuildingContext<out BuildingEventList> {
        return CapturingScriptInvocationBuildingContext(
            mode,
            this.rng,
            this.remainingStackDepth,
            this.onStackOverflowCallback,
            this.ruleStatesHolder
        )
    }

    override fun InvokableRule.onPreInvoke(context: ScriptInvocationBuildingContext<StringBuilder>, state: RuleState) = Unit

    override fun InvokableRule.invokeRule(context: ScriptInvocationBuildingContext<StringBuilder>, state: RuleState) {
        this(context, state)
    }

    override fun InvokableRule.onPostInvoke(context: ScriptInvocationBuildingContext<StringBuilder>, state: RuleState) = Unit

    override fun handleStackOverflow(error: StackOverflowError?) {
        this.onStackOverflowCallback(error)?.let(this::append)
    }

    override fun appendTo(parent: ScriptInvocationBuildingContext<StringBuilder>) {
        this.appendToParentBuilder(parent) { it.append(this.builder) }
    }

    private fun append(sequence: CharSequence) {
        this.builder.append(sequence)
    }

    override fun builderToString(default: () -> String): String = "'${default()}'"
}

private class CapturingScriptInvocationBuildingContext private constructor(
    private val captureModeCallbacks: CaptureModeCallbacks,
    rng: RandomSource,
    remainingStackDepth: Int,
    onStackOverflowCallback: InvocationStackOverflowCallback,
    ruleStatesHolder: RuleStatesHolder,
    builder: MutableList<BuildingEvent> = mutableListOf()
) : ScriptInvocationBuildingContext<MutableList<BuildingEvent>>(rng, remainingStackDepth, onStackOverflowCallback, ruleStatesHolder, builder) {
    private interface CaptureModeCallbacks {
        fun handlePreInvocation(rule: InvokableRule, append: (BuildingEvent) -> Unit)
        fun handleRuleInvocation(rule: InvokableRule, invoker: (InvokableRule) -> Unit)
        fun handlePostInvocation(rule: InvokableRule, append: (BuildingEvent) -> Unit)
        fun handleStackOverflow(append: (BuildingEvent) -> Unit, error: StackOverflowError?, callback: () -> CharSequence?)
    }

    private companion object {
        private sealed interface CaptureInvocationModeCallbacks {
            sealed class EventReturning : CaptureInvocationModeCallbacks {
                abstract fun pre(rule: InvokableRule): BuildingEvent?
                abstract fun post(rule: InvokableRule): BuildingEvent?

                final override fun pre(rule: InvokableRule, append: (BuildingEvent) -> Unit) = this.pre(rule)?.let(append) ?: Unit
                final override fun post(rule: InvokableRule, append: (BuildingEvent) -> Unit) = this.post(rule)?.let(append) ?: Unit
            }

            object RecurseAndKeep : EventReturning() {
                override fun pre(rule: InvokableRule): BuildingEvent = PreInvokeSubRule(rule)
                override fun on(rule: InvokableRule, invoker: (InvokableRule) -> Unit) = invoker(rule)
                override fun post(rule: InvokableRule): BuildingEvent = PostInvokeSubRule(rule)
            }

            object RecurseOnly : EventReturning() {
                override fun pre(rule: InvokableRule): BuildingEvent? = null
                override fun on(rule: InvokableRule, invoker: (InvokableRule) -> Unit) = invoker(rule)
                override fun post(rule: InvokableRule): BuildingEvent? = null
            }

            object NoRecursion : EventReturning() {
                override fun pre(rule: InvokableRule): BuildingEvent = PreInvokeSubRule(rule)
                override fun on(rule: InvokableRule, invoker: (InvokableRule) -> Unit) = Unit
                override fun post(rule: InvokableRule): BuildingEvent = PostInvokeSubRule(rule)
            }

            object NoRecursionPreOnly : EventReturning() {
                override fun pre(rule: InvokableRule): BuildingEvent = PreInvokeSubRule(rule)
                override fun on(rule: InvokableRule, invoker: (InvokableRule) -> Unit) = Unit
                override fun post(rule: InvokableRule): BuildingEvent? = null
            }

            fun pre(rule: InvokableRule, append: (BuildingEvent) -> Unit)
            fun on(rule: InvokableRule, invoker: (InvokableRule) -> Unit)
            fun post(rule: InvokableRule, append: (BuildingEvent) -> Unit)
        }

        private sealed interface CaptureStackOverflowModeCallbacks {
            sealed class EventReturning : CaptureStackOverflowModeCallbacks {
                abstract fun on(error: StackOverflowError?, callback: () -> CharSequence?): BuildingEvent?

                final override fun on(error: StackOverflowError?, callback: () -> CharSequence?, append: (BuildingEvent) -> Unit) =
                    this.on(error, callback)?.let(append) ?: Unit
            }

            sealed class Invoking : EventReturning() {
                abstract fun success(error: StackOverflowError?, message: CharSequence?): BuildingEvent?
                abstract fun failure(throwable: Throwable, error: StackOverflowError?, callback: () -> CharSequence?): BuildingEvent?

                final override fun on(error: StackOverflowError?, callback: () -> CharSequence?): BuildingEvent? {
                    return runCatching(callback).fold(
                        onSuccess = { this.success(error, it) },
                        onFailure = { this.failure(it, error, callback) }
                    )
                }
            }

            object Keep : EventReturning() {
                override fun on(error: StackOverflowError?, callback: () -> CharSequence?): BuildingEvent = RaiseStackOverflow(error, callback)
            }

            object LiteralOrKeep : Invoking() {
                override fun success(error: StackOverflowError?, message: CharSequence?): BuildingEvent? = message?.toString()?.let(::AppendLiteral)
                override fun failure(throwable: Throwable, error: StackOverflowError?, callback: () -> CharSequence?): BuildingEvent =
                    RaiseStackOverflow(error, callback)
            }

            object LiteralOrThrow : Invoking() {
                override fun success(error: StackOverflowError?, message: CharSequence?): BuildingEvent? = message?.toString()?.let(::AppendLiteral)
                override fun failure(throwable: Throwable, error: StackOverflowError?, callback: () -> CharSequence?): Nothing = throw throwable
            }

            object ThrowOnly : Invoking() {
                override fun success(error: StackOverflowError?, message: CharSequence?): BuildingEvent? = null
                override fun failure(throwable: Throwable, error: StackOverflowError?, callback: () -> CharSequence?): Nothing = throw throwable
            }

            fun on(error: StackOverflowError?, callback: () -> CharSequence?, append: (BuildingEvent) -> Unit)
        }

        private class CaptureCallbacks(
            private val invocation: CaptureInvocationModeCallbacks,
            private val stackOverflow: CaptureStackOverflowModeCallbacks
        ) : CaptureModeCallbacks {
            override fun handlePreInvocation(rule: InvokableRule, append: (BuildingEvent) -> Unit) = this.invocation.pre(rule, append)
            override fun handleRuleInvocation(rule: InvokableRule, invoker: (InvokableRule) -> Unit) = this.invocation.on(rule, invoker)
            override fun handlePostInvocation(rule: InvokableRule, append: (BuildingEvent) -> Unit) = this.invocation.post(rule, append)

            override fun handleStackOverflow(append: (BuildingEvent) -> Unit, error: StackOverflowError?, callback: () -> CharSequence?) =
                this.stackOverflow.on(error, callback, append)
        }

        val CaptureMode.callbacks: CaptureModeCallbacks
            get() = CaptureCallbacks(this.invocationMode.callbacks, this.stackOverflowMode.callbacks)

        private val CaptureInvocationMode.callbacks: CaptureInvocationModeCallbacks
            get() = when (this) {
                CaptureInvocationMode.RECURSE_AND_KEEP -> CaptureInvocationModeCallbacks.RecurseAndKeep
                CaptureInvocationMode.RECURSE_ONLY -> CaptureInvocationModeCallbacks.RecurseOnly
                CaptureInvocationMode.NO_RECURSION -> CaptureInvocationModeCallbacks.NoRecursion
                CaptureInvocationMode.NO_RECURSION_PRE_ONLY -> CaptureInvocationModeCallbacks.NoRecursionPreOnly
            }

        private val CaptureStackOverflowMode.callbacks: CaptureStackOverflowModeCallbacks
            get() = when (this) {
                CaptureStackOverflowMode.KEEP -> CaptureStackOverflowModeCallbacks.Keep
                CaptureStackOverflowMode.LITERAL_OR_KEEP -> CaptureStackOverflowModeCallbacks.LiteralOrKeep
                CaptureStackOverflowMode.LITERAL_OR_THROW -> CaptureStackOverflowModeCallbacks.LiteralOrThrow
                CaptureStackOverflowMode.THROW_ONLY -> CaptureStackOverflowModeCallbacks.ThrowOnly
            }
    }

    constructor(
        captureMode: CaptureMode,
        rng: RandomSource,
        remainingStackDepth: Int,
        onStackOverflowCallback: InvocationStackOverflowCallback,
        ruleStatesHolder: RuleStatesHolder
    ) : this(captureMode.callbacks, rng, remainingStackDepth, onStackOverflowCallback, ruleStatesHolder)

    override fun append(literal: String) {
        this.append(AppendLiteral(literal))
    }

    override fun createInvocationSubContext(newStack: Int): ScriptInvocationBuildingContext<MutableList<BuildingEvent>> {
        return CapturingScriptInvocationBuildingContext(
            this.captureModeCallbacks,
            this.rng,
            newStack,
            this.onStackOverflowCallback,
            this.ruleStatesHolder,
            this.builder
        )
    }

    override fun createCapturingSubContext(mode: CaptureMode): ScriptInvocationBuildingContext<out BuildingEventList> {
        return CapturingScriptInvocationBuildingContext(
            mode,
            this.rng,
            this.remainingStackDepth,
            this.onStackOverflowCallback,
            this.ruleStatesHolder
        )
    }

    override fun InvokableRule.onPreInvoke(context: ScriptInvocationBuildingContext<MutableList<BuildingEvent>>, state: RuleState) {
        this.callback(CaptureModeCallbacks::handlePreInvocation, CapturingScriptInvocationBuildingContext::append)
    }

    override fun InvokableRule.invokeRule(context: ScriptInvocationBuildingContext<MutableList<BuildingEvent>>, state: RuleState) {
        this.callback(CaptureModeCallbacks::handleRuleInvocation) { it(context, state) }
    }

    override fun InvokableRule.onPostInvoke(context: ScriptInvocationBuildingContext<MutableList<BuildingEvent>>, state: RuleState) {
        this.callback(CaptureModeCallbacks::handlePostInvocation, CapturingScriptInvocationBuildingContext::append)
    }

    override fun handleStackOverflow(error: StackOverflowError?) {
        this.captureModeCallbacks.handleStackOverflow(this::append, error) { this.onStackOverflowCallback(error) }
    }

    override fun appendTo(parent: ScriptInvocationBuildingContext<MutableList<BuildingEvent>>) {
        this.appendToParentBuilder(parent) { it.addAll(this.builder) }
    }

    private fun append(event: BuildingEvent) {
        this.builder.add(event)
    }

    private inline fun <A> InvokableRule.callback(
        callbackChoiceHandler: CaptureModeCallbacks.(InvokableRule, (A) -> Unit) -> Unit,
        crossinline handler: CapturingScriptInvocationBuildingContext.(A) -> Unit
    ) {
        this@CapturingScriptInvocationBuildingContext.captureModeCallbacks.callbackChoiceHandler(this) {
            this@CapturingScriptInvocationBuildingContext.handler(it)
        }
    }

    override fun builderToString(default: () -> String): String = default()
}

private class ScriptInvocationBuildingContextBootstrapper(
    private val start: InvokableRule,
    private val randomSource: RandomSource,
    private val maxStack: Int,
    private val stackOverflowCallback: InvocationStackOverflowCallback,
    private val ruleStatesHolder: RuleStatesHolder = RuleStatesHolder()
) {
    init {
        require(this.maxStack > 0) { "A stack depth of ${this.maxStack} is too small for rule invocation to occur: it must be at least 1" }
    }

    fun next(): String {
        val rootBuilder = StringBuilder()
        val initialContext = this.createInitialContext(rootBuilder)
        initialContext.invoke(this.start)
        return rootBuilder.toString()
    }

    private fun createInitialContext(rootBuilder: StringBuilder): ScriptInvocationBuildingContext<StringBuilder> {
        return RegularScriptInvocationBuildingContext(
            rng = this.randomSource,
            remainingStackDepth = this.maxStack,
            onStackOverflowCallback = this.stackOverflowCallback,
            ruleStatesHolder = this.ruleStatesHolder,
            builder = rootBuilder
        )
    }

    override fun toString(): String =
        "Bootstrapper for context of ${this.start} with rng=${this.randomSource}, maxStack=${this.maxStack}, " +
            "callback=${this.stackOverflowCallback}, states=${this.ruleStatesHolder}"
}

private class ApiRandomSourceAdapter(private val rng: net.thesilkminer.babelk.api.invoke.RandomSource) : RandomSource {
    override fun nextBits(count: Int): Int = this.rng.nextBits(count)
    override fun nextBoolean(): Boolean = this.rng.nextBoolean()
    override fun nextInt(): Int = this.rng.nextInt()
    override fun nextIntInRange(begin: Int, end: Int): Int = this.rng.nextIntInRange(begin, end)
    override fun nextLong(): Long = this.rng.nextLong()
    override fun nextLongInRange(begin: Long, end: Long): Long = this.rng.nextLongInRange(begin, end)
    override fun nextFloat(): Float = this.rng.nextFloat()
    override fun nextFloatInRange(begin: Float, end: Float): Float = this.rng.nextFloatInRange(begin, end)
    override fun nextDouble(): Double = this.rng.nextDouble()
    override fun nextDoubleInRange(begin: Double, end: Double): Double = this.rng.nextDoubleInRange(begin, end)
    override fun nextBytes(count: Int): ByteArray = this.rng.nextBytes(count)
    override fun nextSeededRandomSource(): RandomSource = this.rng.nextSeededRandomSource().let(::ApiRandomSourceAdapter)
    override fun toString(): String = this.rng.toString()
}

private class ExternalArgumentRule(private val value: String) : Rule {
    override fun append(context: BuildingContext, state: RuleState, rng: net.thesilkminer.babelk.script.api.invoke.RandomSource, arguments: InvocationArguments) {
        context.append(this.value)
    }

    override fun toString(): String = "ExternalArgument[${this.value}]"
}

internal operator fun NamedRule.invoke(grammarRule: GrammarRule, configuration: InvocationConfiguration): Sequence<String> {
    val rule = this.toInvokableRuleWith(configuration.ruleArguments)
    val contextBootstrapper = configuration.makeBootstrapperFor(grammarRule, rule)
    return sequence {
        while (true) {
            yield(contextBootstrapper.next())
        }
    }
}

private fun Rule.toInvokableRuleWith(args: Map<String, String>): InvokableRule {
    return InvokableRule(this.provider, args.toInvocationArguments())
}

private fun Map<String, String>.toInvocationArguments(): InvocationArguments {
    return this.map { (name, value) -> ArgumentName(name) to ExternalArgumentRule(value).toInvokableRuleWith(mapOf()) }
        .toTypedArray()
        .let { InvocationArguments(*it) }
}

private fun InvocationConfiguration.makeBootstrapperFor(grammarRule: GrammarRule, rule: InvokableRule): ScriptInvocationBuildingContextBootstrapper {
    val randomSource = ApiRandomSourceAdapter(this.randomSource)
    val maxStack = this.sequenceConfiguration.stackDepth
    val stackOverflowCallback = this.sequenceConfiguration.onStackOverflow.toInvocationCallback(grammarRule)
    return ScriptInvocationBuildingContextBootstrapper(rule, randomSource, maxStack, stackOverflowCallback)
}

private fun StackOverflowCallback.toInvocationCallback(grammarRule: GrammarRule): InvocationStackOverflowCallback = { this(grammarRule, it) }

private val Rule.provider: Provider<Rule> get() = Provider { this }
