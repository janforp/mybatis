

package org.apache.ibatis.submitted.extendresultmap;

import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Select;

public interface TestMapperY {

    @Select("SELECT * FROM test AS t LIMIT 1")
    @ResultMap("map")
    TestModel retrieveTestString();
}
