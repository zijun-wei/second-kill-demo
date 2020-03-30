package com.seckill.service.bos;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @Title ItemBO
 * @Description 商品类
 * @Author Zijun_Wei
 * @Date 2020/2/2
 */
@Data
public class ItemBO implements Serializable {
    private Integer id;
    /**商品名*/
    @NotBlank(message = "商品名称不能为空")
    private String title;
    /**商品价格*/
    @NotNull(message = "商品价格不能为空")
    @Min(value = 0, message = "商品价格必须大于0")
    private BigDecimal price;
    /**商品库存*/
    @NotNull(message = "商品库存不能为空")
    private Integer stock;
    /**商品描述*/
    @NotBlank(message = "商品描述信息不能为空")
    private String description;
    /**商品销量*/
    private Integer sales;
    /**商品描述图片的url*/
    @NotBlank(message = "商品图片信息不能为空")
    private String imgUrl;

    /**使用聚合模型，如果秒杀属性不为空，表示还有未截止的秒杀活动，这里的设计很精妙*/
    private KillBO killBO;
}
