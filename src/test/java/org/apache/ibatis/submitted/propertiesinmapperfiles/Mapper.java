package org.apache.ibatis.submitted.propertiesinmapperfiles;

import org.apache.ibatis.annotations.Param;

public interface Mapper {

    User getUser(Integer id);

    User getByCrit(@Param("id") Integer id, @Param("name") String name);
}
