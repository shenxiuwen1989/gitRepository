package com.hu.tran.xserver.pack;

import lombok.Data;

/**
 *  xml字段对象
 * @author hutiantian
 * @create 2018/6/8 19:29
 * @since 1.0.0
 */
@Data
public class Field {
    private String name;			//字段名称
    private String desc;			//字段描述
    private int len;				//字段长度
    private String loop;			//循环域名称
    private String nullable;		//是否允许为空，用于校验请求私有域，响应不校验
    private String tag;				//对应xml的标签
}
