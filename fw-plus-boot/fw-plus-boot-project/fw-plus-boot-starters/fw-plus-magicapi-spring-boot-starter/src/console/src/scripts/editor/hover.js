import $i from '../i18n.js'
import tokenizer from '../parsing/tokenizer.js'
import { TokenStream } from '../parsing/index.js'
import {
    ClassConverter,
    FunctionCall,
    LinqSelect,
    MapOrArrayAccess,
    MemberAccess, NewStatement,
    Node,
    VarDefine,
    VariableAccess
} from '../parsing/ast.js'
import { Parser } from '..//parsing/parser.js'
import { Range } from 'monaco-editor'
import JavaClass from "./java-class"

const findBestMatch = (node, row, col) => {
    let expressions = node.expressions().filter(it => it);
    for (let index in expressions) {
        let expr = expressions[index];
        if (expr instanceof FunctionCall && expr.target instanceof VariableAccess && expr.getSpan().inPosition(row, col)) {
            return expr;
        }
        let v = findBestMatch(expr, row, col)
        if (v instanceof Node) {
            return v;
        }
    }
    if (node.getSpan().inPosition(row, col)) {
        return node;
    }
    return null;
}
const generateMethodDocument = (prefix, method, contents) => {
    contents.push({ value: `${prefix}${method.fullName}` })
    method.comment && contents.push({ value: `${method.comment}` })
    method.parameters.forEach((param, pIndex) => {
        if (pIndex > 0 || !method.extension) {
            contents.push({ value: `${param.name}：${(param.comment || param.type)}` })
        }
    })
    contents.push({ value: $i('editor.hover.returnType', method.returnType) })
}

const generateFunctionCall = (methodName, env, contents, isNew) => {
    let functions = JavaClass.findFunction().filter(method => method.name === methodName);
    if (functions.length > 0) {
        generateMethodDocument('', functions[0], contents);
    } else {
        let value = env[methodName];
        if (value && value.indexOf('@') === 0) {
            let functionName = value.substring(1);
            let func = JavaClass.getOnlineFunction(functionName);
            if (func) {
                let parameters = Array.isArray(func.parameter) ? func.parameter : JSON.parse(func.parameter || '[]');
                parameters.forEach(it => it.comment = it.description);
                generateMethodDocument('', {
                    fullName: methodName + " " + func.name,
                    comment: func.description || '',
                    parameters,
                    returnType: func.returnType
                }, contents);
            }

        } else {
            contents.push({ value: isNew ? $i('editor.hover.createObject', methodName) : $i('editor.hover.accessVariable', methodName) })
            contents.push({ value: $i('editor.hover.type', value || $i('editor.hover.unknow')) })
        }
    }
}
const HoverProvider = {
    provideHover: async (model, position) => {
        let value = model.getValue()
        let tokens = tokenizer(value);
        let tokenStream = new TokenStream(tokens);
        let parser = new Parser(tokenStream)
        let nodes = parser.parse(true);
        let input = model.getValueInRange({
            startLineNumber: 1,
            startColumn: 1,
            endLineNumber: position.lineNumber,
            endColumn: position.column
        });
        let index = input.length;
        for (let i = 0, len = nodes.length; i < len; i++) {
            let best = parser.findBestMatch(nodes[i], index)
            if (best) {
                let env = await parser.processEnv(nodes)
                let contents = [];
                let line = best.getSpan().getLine();
                if (best instanceof VarDefine) {
                    let value = env[best.getVarName()];
                    contents.push({ value: $i('editor.hover.defineVariable', best.getVarName()) })
                    contents.push({ value: $i('editor.hover.variableType', value) })
                } else if (best instanceof ClassConverter) {
                    if (best.convert === 'json') {
                        contents.push({ value: $i('editor.hover.convertToJson') })
                    } else if (best.convert === 'stringify') {
                        contents.push({ value: $i('editor.hover.convertToJsonString') })
                    } else if (best.convert === 'sql') {
                        let args = best.args || []
                        contents.push({ value: $i('editor.hover.convertToSql', args[0]?.span?.getText()?.toUpperCase(), best.target.getSpan().getText()) })
                    } else {
                        contents.push({ value: $i('editor.hover.convertTo', best.convert) })
                    }
                } else if (best instanceof VariableAccess) {
                    let value = env[best.getVariable()];
                    if (value) {
                        contents.push({ value: $i('editor.hover.accessVariable', best.getVariable()) })
                        contents.push({ value: $i('editor.hover.variableType', value || $i('editor.hover.unknow')) })
                    } else {
                        generateFunctionCall(best.getVariable(), env, contents)
                    }
                } else if (best instanceof MemberAccess) {
                    let javaType = await best.getTarget().getJavaType(env);
                    let clazz = await JavaClass.loadClass(javaType);
                    let memberName = best.member.getText()
                    JavaClass.findMethods(clazz).filter(method => method.name === memberName)
                        .forEach(method => generateMethodDocument(`${JavaClass.getSimpleClass(javaType)}.`, method, contents))
                    JavaClass.findEnums(clazz).filter(it => it === memberName).forEach(it => {
                        contents.push({ value: $i('editor.hover.accessEnum', javaType, memberName) })
                    })
                    JavaClass.findAttributes(clazz).filter(attr => attr.name === memberName).forEach(it => {
                        contents.push({ value: $i('editor.hover.accessAttribute', javaType, memberName) })
                        it.comment && contents.push({ value: `${it.comment}` })
                        contents.push({ value: $i('editor.hover.attributeType', it.type) })
                    })
                    line = best.member.getLine();
                } else if (best instanceof FunctionCall) {
                    let target = best.target;
                    generateFunctionCall(target.variable, env, contents)
                } else if (best instanceof NewStatement) {
                    let target = best.identifier;
                    if (target instanceof VariableAccess) {
                        generateFunctionCall(target, env, contents, true)
                    }
                } else if (best instanceof MapOrArrayAccess) {
                    contents.push({ value: $i('editor.hover.accessMapOrArray') })
                } else if (best instanceof LinqSelect) {
                    contents.push({ value: $i('editor.hover.linqQuery') })
                } else {
                    return;
                }
                return {
                    range: new Range(line.lineNumber, line.startCol, line.endLineNumber, line.endCol + 1),
                    contents
                };
            }
        }
    }
}
export default HoverProvider