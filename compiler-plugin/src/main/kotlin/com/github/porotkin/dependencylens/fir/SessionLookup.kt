package com.github.porotkin.dependencylens.fir

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.lookupSuperTypes
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import java.util.concurrent.ConcurrentHashMap

internal class SessionLookup(
    private val session: FirSession,
) {
    private val supertypes = ConcurrentHashMap<ClassId, List<Pair<KotlinLibrary, String>>>()

    private val packages = ConcurrentHashMap<FqName, List<KotlinLibrary?>>()

    fun classLike(classId: ClassId): FirClassLikeSymbol<*>? =
        session.symbolProvider.getClassLikeSymbolByClassId(classId)

    fun symbol(type: ConeClassLikeType): FirClassLikeSymbol<*>? =
        type.lookupTag.toSymbol(session)

    fun topLevel(
        pkg: FqName,
        name: Name,
    ): List<FirBasedSymbol<*>> =
        listOfNotNull(classLike(ClassId(pkg, name))) +
            session.symbolProvider.getTopLevelCallableSymbols(pkg, name)

    fun packageLibraries(pkg: FqName): List<KotlinLibrary?> =
        packages.getOrPut(pkg) { librariesIn(pkg) }

    fun supertypeLibraries(symbol: FirClassLikeSymbol<*>): List<Pair<KotlinLibrary, String>> =
        supertypes.getOrPut(symbol.classId) { librariesAbove(symbol) }

    private fun librariesIn(pkg: FqName): List<KotlinLibrary?> {
        val names = session.symbolProvider.symbolNamesProvider
        val classifiers = names.getTopLevelClassifierNamesInPackage(pkg) ?: return emptyList()
        val callables = names.getTopLevelCallableNamesInPackage(pkg) ?: return emptyList()

        return (classifiers + callables)
            .flatMap { topLevel(pkg, it) }
            .map { it.library }
    }

    private fun librariesAbove(symbol: FirClassLikeSymbol<*>): List<Pair<KotlinLibrary, String>> =
        lookupSuperTypes(symbol, lookupInterfaces = true, deep = true, useSiteSession = session)
            .mapNotNull(::symbol)
            .mapNotNull { supertype ->
                supertype.library?.let { it to "${supertype.classId.asString()} (supertype of ${symbol.classId.asString()})" }
            }
}
