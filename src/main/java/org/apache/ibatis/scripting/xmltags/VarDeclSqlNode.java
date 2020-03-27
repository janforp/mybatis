package org.apache.ibatis.scripting.xmltags;

/**
 * @author Frank D. Martinez [mnesarco]
 */
public class VarDeclSqlNode implements SqlNode {

    private final String name;

    private final String expression;

    /**
     * <bind name="pattern" value="'%' + _parameter + '%'" />
     *
     * @param var pattern
     * @param exp '%' + _parameter + '%'
     */
    public VarDeclSqlNode(String var, String exp) {
        name = var;
        expression = exp;
    }

    @Override
    public boolean apply(DynamicContext context) {
        final Object value = OgnlCache.getValue(expression, context.getBindings());
        context.bind(name, value);
        return true;
    }
}
