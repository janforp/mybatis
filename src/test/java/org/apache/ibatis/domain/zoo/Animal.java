package org.apache.ibatis.domain.zoo;

import lombok.Data;

/**
 * 类说明：
 *
 * @author zhucj
 * @since 2020/3/16 - 下午9:36
 */
@Data
public class Animal {

    private Animal parent;

    private String name;
}
