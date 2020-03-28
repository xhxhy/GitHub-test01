package com.leyou.item.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.leyou.common.pojo.PageResult;
import com.leyou.item.mapper.BrandMapper;
import com.leyou.item.pojo.Brand;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

/**
 * @author: HuYi.Zhang
 * @create: 2018-07-21 14:45
 **/
@Service
public class BrandService {

    @Autowired
    private BrandMapper brandMapper;

    private static final Logger logger = LoggerFactory.getLogger(BrandService.class);

    public PageResult<Brand> queryBrandByPage(Integer page, Integer rows, String sortBy, Boolean desc, String key) {
        // 分页
        PageHelper.startPage(page, rows);
        // 过滤
        Example example = new Example(Brand.class);
        if(StringUtils.isNotBlank(key)){
            example.createCriteria().orLike("name", "%"+key+"%")
                    .orEqualTo("letter", key.toUpperCase());
        }
        // 排序
        if(StringUtils.isNotBlank(sortBy)) {
            String orderByClause = desc ? sortBy + " DESC" : sortBy + " ASC";
            example.setOrderByClause(orderByClause);
        }

        List<Brand> brands = brandMapper.selectByExample(example);

        // 获取分页数据
        PageInfo<Brand> info = new PageInfo<>(brands);

        // 封装结果
        return new PageResult<>(info.getTotal(), info.getList());
    }

    @Transactional
    public void saveBrand(Brand brand, List<Long> cids) {
        try{
            // 保存Brand对象
            brand.setId(null);
            brandMapper.insert(brand);

            // 保存Category和Brand的中间表对象
            for (Long cid : cids) {
                brandMapper.insertCategoryBrand(cid, brand.getId());
            }
        }catch (Exception e){
            logger.error("保存品牌及品牌和分类中间表失败，brandId:{}, cids:{}", brand.getId(), cids);
            throw new RuntimeException(e);
        }
    }

    public Brand queryById(Long id){
        return brandMapper.selectByPrimaryKey(id);
    }

    public List<Brand> queryBrandByCid(Long cid) {
        return brandMapper.queryByCategoryId(cid);
    }

    public List<Brand> queryByIds(List<Long> ids) {
        return brandMapper.selectByIdList(ids);
    }
}
