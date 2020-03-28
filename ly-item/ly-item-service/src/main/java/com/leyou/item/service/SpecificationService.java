package com.leyou.item.service;

import com.leyou.item.mapper.SpecGroupMapper;
import com.leyou.item.mapper.SpecParamMapper;
import com.leyou.item.pojo.SpecGroup;
import com.leyou.item.pojo.SpecParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: HuYi.Zhang
 * @create: 2018-07-22 16:03
 **/
@Service
public class SpecificationService {

    @Autowired
    private SpecGroupMapper groupMapper;

    @Autowired
    private SpecParamMapper paramMapper;

    public List<SpecGroup> queryGroupByCid(Long cid) {
        SpecGroup t = new SpecGroup();
        t.setCid(cid);
        return groupMapper.select(t);
    }

    public List<SpecParam> queryParam(Long gid, Long cid, Boolean generic, Boolean searching) {
        SpecParam t = new SpecParam();
        t.setGroupId(gid);
        t.setCid(cid);
        t.setGeneric(generic);
        t.setSearching(searching);
        return paramMapper.select(t);
    }

    public List<SpecGroup> querySpecsByCid(Long cid) {
        // 先查询组
        List<SpecGroup> groups = queryGroupByCid(cid);
        // 查询当前分类下的所有参数
        List<SpecParam> params = queryParam(null, cid, null, null);
        // 把param放入一个Map中，key是组id，值是组内的所有参数
        Map<Long, List<SpecParam>> map = new HashMap<>();
        for (SpecParam param : params) {
            // 判断当前参数所属的组在map中是否存在
            if (!map.containsKey(param.getGroupId())) {
                map.put(param.getGroupId(), new ArrayList<>());
            }
            // 存param到集合
            map.get(param.getGroupId()).add(param);
        }
        // 循环存储param数据
        for (SpecGroup group : groups) {
            group.setParams(map.get(group.getId()));
        }
        return groups;
    }
}
