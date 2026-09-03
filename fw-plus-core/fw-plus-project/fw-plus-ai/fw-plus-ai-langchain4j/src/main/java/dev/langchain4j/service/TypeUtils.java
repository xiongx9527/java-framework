package dev.langchain4j.service;

import dev.langchain4j.Internal;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.data.message.ImageContent;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.List;
import static dev.langchain4j.internal.Exceptions.illegalArgument;
import static dev.langchain4j.internal.Utils.repeat;
import static dev.langchain4j.internal.ValidationUtils.ensureNotEmpty;
import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;

@Internal public class TypeUtils {
 public TypeUtils(){}
 public static Class<?> getRawClass(Type t){if(t==null)throw new NullPointerException("Type should not be null.");if(t instanceof Class<?>)return(Class<?>)t;if(t instanceof ParameterizedType)return(Class<?>)((ParameterizedType)t).getRawType();throw new IllegalArgumentException("Unable to extract raw class.");}
 public static boolean typeHasRawClass(Type t,Class<?> c){return t!=null&&c!=null&&c.equals(getRawClass(t));}
 public static Class<?> resolveFirstGenericParameterClass(Type t){Type[]a=args(t);if(a.length==0)return null;Type f=a[0];if(f instanceof Class<?>)return(Class<?>)f;if(f instanceof ParameterizedType)return(Class<?>)((ParameterizedType)f).getRawType();return null;}
 public static Type resolveFirstGenericParameterType(Type t){Type[]a=args(t);return a.length==0?null:a[0];}
 private static Type[] args(Type t){ensureNotNull(t,"type");if(!(t instanceof ParameterizedType))return new Type[0];Type[]a=((ParameterizedType)t).getActualTypeArguments();ensureNotEmpty(a,"%s","Parameterized type has no type arguments.");return a;}
 public static void validateReturnTypesAreProperlyParametrized(String n,Type t){validate(n,t,new ArrayList<Type>());}
 private static void validate(String n,Type t,List<Type> chain){if(t instanceof ParameterizedType){ParameterizedType p=(ParameterizedType)t;for(Type a:p.getActualTypeArguments()){List<Type>b=new ArrayList<Type>(chain);b.add(p);validate(n,a,b);}}else if(t instanceof WildcardType||t instanceof TypeVariable||t instanceof Class<?>&&((Class<?>)t).getTypeParameters().length>0){chain.add(t);throw illegalArgument("The return type '%s' of the method '%s' must be parameterized with a concrete type, for example: %s or %s",declaration(chain,null),n,declaration(chain,"String"),declaration(chain,"MyCustomPojo"));}}
 private static String declaration(List<Type> chain,String leaf){StringBuilder b=new StringBuilder();int raw=0;for(Type t:chain){if(leaf!=null&&(t instanceof WildcardType||t instanceof TypeVariable))continue;if(b.length()>0)b.append('<');if(leaf==null&&t instanceof WildcardType)b.append('?');else if(leaf==null&&t instanceof TypeVariable)b.append(t.getTypeName());else{b.append(getRawClass(t).getSimpleName());raw++;}}if(leaf!=null)b.append('<').append(leaf).append(repeat(">",raw));else b.append(repeat(">",Math.max(0,chain.size()-1)));return b.toString();}
 public static boolean isImageType(Class<?> c){return Image.class.isAssignableFrom(c)||ImageContent.class.isAssignableFrom(c);}
}
