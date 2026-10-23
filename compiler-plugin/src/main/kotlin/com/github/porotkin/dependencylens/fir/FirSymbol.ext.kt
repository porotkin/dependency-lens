package com.github.porotkin.dependencylens.fir

import org.jetbrains.kotlin.fir.declarations.utils.sourceElement
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.unwrapFakeOverrides
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.metadata.KlibDeserializedContainerSource

internal val FirBasedSymbol<*>.library: KotlinLibrary?
    get() {
        val source = when (this) {
            is FirClassLikeSymbol<*> -> sourceElement
            is FirCallableSymbol<*> -> unwrapFakeOverrides().containerSource
            else -> null
        }

        return (source as? KlibDeserializedContainerSource)?.klib
    }

internal val FirCallableSymbol<*>.signatureTypes: List<ConeKotlinType?>
    get() = listOf(resolvedReturnType, resolvedReceiverType, dispatchReceiverType) +
        contextParameterSymbols.map { it.resolvedReturnType } +
        (this as? FirFunctionSymbol<*>)?.valueParameterSymbols.orEmpty().map { it.resolvedReturnType }
