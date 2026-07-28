/* Copyright (c) 2020 vesoft inc. All rights reserved.
 *
 * This source code is licensed under Apache 2.0 License.
 */

package com.xiongx9527.fw.plus.graph.nebula.jdbc.utils;

import com.xiongx9527.fw.plus.graph.jdbc.GraphJdbcDefaults;
import java.sql.SQLFeatureNotSupportedException;

public class ExceptionBuilder {

    private ExceptionBuilder() {}

    /**
     * An {@link SQLFeatureNotSupportedException} exception builder that  retrieve it's caller to make
     * a not yet implemented exception with method and class name.
     *
     * @return an SQLFeatureNotSupportedException
     */
    public static SQLFeatureNotSupportedException buildUnsupportedOperationException() {
        return (SQLFeatureNotSupportedException) GraphJdbcDefaults.unsupportedOperation();
    }

}
