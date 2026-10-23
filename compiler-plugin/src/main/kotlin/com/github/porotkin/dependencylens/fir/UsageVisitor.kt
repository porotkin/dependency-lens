package com.github.porotkin.dependencylens.fir

import com.github.porotkin.dependencylens.Kind
import com.github.porotkin.dependencylens.Usage
import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirResolvedImport
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.UnresolvedExpressionTypeAccess
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.scopes.impl.typeAliasConstructorInfo
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeDefinitelyNotNullType
import org.jetbrains.kotlin.fir.types.ConeFlexibleType
import org.jetbrains.kotlin.fir.types.ConeIntersectionType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.abbreviatedType
import org.jetbrains.kotlin.fir.types.customAnnotations
import org.jetbrains.kotlin.fir.types.type
import org.jetbrains.kotlin.fir.unwrapFakeOverrides
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.ClassIdBasedLocality

internal class UsageVisitor(
    private val lookup: SessionLookup,
    private val usage: Usage,
    private val file: String,
) : FirVisitorVoid() {
    @OptIn(UnresolvedExpressionTypeAccess::class)
    override fun visitElement(element: FirElement) {
        when (element) {
            is FirResolvedImport -> import(element)
            is FirResolvedNamedReference -> symbol(element.resolvedSymbol)
            is FirResolvedQualifier -> element.qualifierSymbol?.let { classLike(it, Kind.DIRECT) }
            is FirResolvedTypeRef -> type(element.coneType, element.usageKind)
            else -> Unit
        }

        if (element is FirExpression) {
            type(element.coneTypeOrNull, Kind.SIGNATURE)
        }

        element.acceptChildren(this)
    }

    private fun import(import: FirResolvedImport) {
        val (libraries, example) = import.candidates() ?: return
        val onlyLibrary = libraries.distinctBy { it?.path }.singleOrNull()

        record(onlyLibrary, Kind.IMPORT) { example }
    }

    private fun FirResolvedImport.candidates(): Pair<List<KotlinLibrary?>, String>? {
        val pkg = packageFqName
        val parent = resolvedParentClassId

        if (isAllUnder) {
            return if (parent == null) lookup.packageLibraries(pkg) to "$pkg.*" else null
        }

        val name = importedName ?: return null
        val classId = parent?.createNestedClassId(name) ?: ClassId(pkg, name)
        val symbols = if (parent != null) listOfNotNull(lookup.classLike(classId)) else lookup.topLevel(pkg, name)

        return symbols.map { it.library } to classId.asString()
    }

    private fun symbol(symbol: FirBasedSymbol<*>) {
        when (symbol) {
            is FirClassLikeSymbol<*> -> classLike(symbol, Kind.DIRECT)
            is FirCallableSymbol<*> -> callable(symbol)
            else -> Unit
        }
    }

    private fun classLike(
        symbol: FirClassLikeSymbol<*>,
        kind: Kind,
    ) {
        record(symbol.library, kind) { symbol.classId.asString() }

        if (symbol is FirTypeAliasSymbol) {
            type(symbol.resolvedExpandedTypeRef.coneType, Kind.SIGNATURE)
        }

        for ((library, example) in lookup.supertypeLibraries(symbol)) {
            record(library, Kind.SUPERTYPE) { example }
        }
    }

    @OptIn(ClassIdBasedLocality::class)
    private fun callable(symbol: FirCallableSymbol<*>) {
        val original = symbol.unwrapFakeOverrides()
        record(original.library, Kind.DIRECT) { original.callableId.toString() }

        original.callableId?.classId
            ?.takeUnless { it.isLocal }
            ?.let(lookup::classLike)
            ?.let { classLike(it, Kind.DIRECT) }

        (symbol as? FirConstructorSymbol)?.typeAliasConstructorInfo?.typeAliasSymbol
            ?.let { classLike(it, Kind.DIRECT) }

        symbol.signatureTypes.forEach { type(it, Kind.SIGNATURE) }
    }

    private fun type(
        cone: ConeKotlinType?,
        kind: Kind,
    ) {
        when (cone) {
            is ConeFlexibleType -> {
                type(cone.lowerBound, kind)
                type(cone.upperBound, kind)
            }

            is ConeDefinitelyNotNullType -> type(cone.original, kind)
            is ConeIntersectionType -> cone.intersectedTypes.forEach { type(it, kind) }
            is ConeClassLikeType -> classLikeType(cone, kind)
            else -> Unit
        }

        cone?.customAnnotations?.forEach { annotation ->
            type((annotation.annotationTypeRef as? FirResolvedTypeRef)?.coneType, Kind.SIGNATURE)
        }
    }

    private fun classLikeType(
        cone: ConeClassLikeType,
        kind: Kind,
    ) {
        val alias = cone.abbreviatedType

        if (alias != null && kind == Kind.DIRECT) {
            type(alias, Kind.DIRECT)
        }

        val expandedKind = if (alias != null) Kind.SIGNATURE else kind
        lookup.symbol(cone)?.let { classLike(it, expandedKind) }
        cone.typeArguments.forEach { type(it.type, expandedKind) }
    }

    private fun record(
        library: KotlinLibrary?,
        kind: Kind,
        example: () -> String,
    ) {
        library ?: return
        usage.record(file, library, kind, example)
    }
}

private val FirResolvedTypeRef.usageKind: Kind
    get() = if (source?.kind is KtRealSourceElementKind) Kind.DIRECT else Kind.SIGNATURE
