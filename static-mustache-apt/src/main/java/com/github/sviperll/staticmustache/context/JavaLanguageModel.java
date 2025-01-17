/*
 * Copyright (c) 2015, Victor Nazarov <asviraspossible@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation and/or
 *     other materials provided with the distribution.
 *
 *  3. Neither the name of the copyright holder nor the names of its contributors
 *     may be used to endorse or promote products derived from this software
 *     without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 *  THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 *  IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 *  EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.github.sviperll.staticmustache.context;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import org.eclipse.jdt.annotation.Nullable;

import com.github.sviperll.staticmustache.context.types.KnownType;
import com.github.sviperll.staticmustache.context.types.KnownTypes;
import com.github.sviperll.staticmustache.context.types.NativeType;
import com.github.sviperll.staticmustache.context.types.ObjectType;

/**
 *
 * @author Victor Nazarov <asviraspossible@gmail.com>
 */
public class JavaLanguageModel {
    
    private static JavaLanguageModel INSTANCE;
    
    public static JavaLanguageModel createInstance(Types types, Elements elements) {
        KnownTypes knownTypes = KnownTypes.createInstace(elements, types);
        var self = new JavaLanguageModel(types, elements, knownTypes);
        INSTANCE = self;
        return self;
    }
    
    public static JavaLanguageModel getInstance() {
        return INSTANCE;
    }

    private final Types operations;
    private final Elements elements;
    
    private final KnownTypes knownTypes;
    JavaLanguageModel(Types operations, Elements elements, KnownTypes knownTypes) {
        this.operations = operations;
        this.knownTypes = knownTypes;
        this.elements = elements;
    }
    
    
    public Types getTypes() {
        return operations;
    }
    
    public Elements getElements() {
        return this.elements;
    }

    KnownTypes knownTypes() {
        return knownTypes;
    }

    DeclaredType getDeclaredType(TypeElement element, TypeMirror... typeArguments) {
        return operations.getDeclaredType(element, typeArguments);
    }

    boolean isSameType(TypeMirror first, TypeMirror second) {
        return operations.isSameType(first, second);
    }

    boolean isSubtype(TypeMirror subtype, TypeMirror supertype) {
        return operations.isSubtype(subtype, supertype);
    }

    boolean isUncheckedException(TypeMirror exceptionType) {
        return operations.isAssignable(exceptionType, operations.getDeclaredType(knownTypes._Error.typeElement()))
               || operations.isAssignable(exceptionType, operations.getDeclaredType(knownTypes._RuntimeException.typeElement()));
    }

    TypeMirror getArrayType(TypeMirror elementType) {
        return operations.getArrayType(elementType);
    }

    TypeMirror asMemberOf(DeclaredType containing, Element element) {
        return operations.asMemberOf(containing, element);
    }

    JavaExpression expression(String text, NativeType type) {
        return new JavaExpression(this, text, type.typeMirror(), List.of());
    }
    
    JavaExpression expression(String text, TypeMirror type) {
        return new JavaExpression(this, text, type, List.of());
    }
    
    String eraseType(DeclaredType dt) {
       return operations.erasure(dt).toString();
    }

    TypeMirror getGenericDeclaredType(TypeElement element) {
        List<? extends TypeParameterElement> typeParameters = element.getTypeParameters();
        int numberOfParameters = typeParameters.size();
        List<TypeMirror> typeArguments = new ArrayList<TypeMirror>(numberOfParameters);
        for (int i = 0; i < numberOfParameters; i++) {
            typeArguments.add(operations.getWildcardType(null, null));
        }
        TypeMirror[] typeArgumentArray = new TypeMirror[typeArguments.size()];
        typeArgumentArray = typeArguments.toArray(typeArgumentArray);
        return getDeclaredType(element, typeArgumentArray);
    }

    @Nullable DeclaredType getSupertype(DeclaredType type, ObjectType supertypeDeclaration) {
        return getSupertype(type, supertypeDeclaration.typeElement());
    }
    
    @Nullable DeclaredType getSupertype(DeclaredType type, TypeElement supertypeDeclaration) {
        if (type.asElement().equals(supertypeDeclaration))
            return type;
        else {
            List<? extends TypeMirror> supertypes = operations.directSupertypes(type);
            for (TypeMirror supertype: supertypes) {
                DeclaredType result = getSupertype((DeclaredType)supertype, supertypeDeclaration);
                if (result != null)
                    return result;
            }
            return null;
        }
    }

    TypeElement asElement(DeclaredType declaredType) {
        return (TypeElement)operations.asElement(declaredType);
    }
    
    
    boolean isType(TypeMirror type, KnownType knownType) {
        if (knownType instanceof NativeType nativeType) {
            return isSameType(type, nativeType.typeMirror());
        }
        if (knownType instanceof ObjectType objectType) {
            return isSubtype(type, getDeclaredType(objectType.typeElement()));
        }
        throw new IllegalStateException();
        
    }
    public Optional<KnownType> resolvetype(TypeMirror type) throws TypeException {
        if (type instanceof WildcardType) {
            return resolvetype(((WildcardType)type).getExtendsBound());
        }
        else if (isSubtype(type, getGenericDeclaredType(knownTypes._Renderable.typeElement()))) {
            return  Optional.of(knownTypes._Renderable);
        } 
        for (var nt : knownTypes.getNativeTypes()) {
            if (isType(type, nt)) {
                return Optional.of(nt);
            }
        }
        for (var ot : knownTypes.getObjectTypes()) {
            if (isType(type, ot)) {
                return Optional.of(ot);
            }
        }
        return Optional.empty();
    }
    
}
