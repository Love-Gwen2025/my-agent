package com.ynp.agent.converter;


import com.ynp.agent.model.bo.UserCreateBo;
import com.ynp.agent.model.bo.UserUpdateBo;
import com.ynp.agent.model.domain.User;
import com.ynp.agent.model.param.UserParam;
import com.ynp.agent.model.vo.UserVo;
import jakarta.validation.Valid;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface UserConverter {

    /**
     * 将控制层入参转换为用户新增业务对象。
     *
     * @param inParam 控制层请求参数
     * @return 用户新增业务对象
     */
    UserCreateBo param2CreateBo(@Valid UserParam inParam);

    /**
     * 将控制层入参转换为用户更新业务对象。
     *
     * @param inParam 控制层请求参数
     * @return 用户更新业务对象
     */
    UserUpdateBo param2UpdateBo(@Valid UserParam inParam);

    /**
     * 将新增业务对象转换为领域实体。
     *
     * @param bo 新增业务对象
     * @return 领域实体
     */
    User createBo2Entity(UserCreateBo bo);

    /**
     * 将更新业务对象合并到既有实体中，忽略空值字段。
     *
     * @param bo     更新业务对象
     * @param target 目标实体
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromBo(UserUpdateBo bo, @MappingTarget User target);

    /**
     * 领域实体转换为视图对象，供控制层返回使用。
     *
     * @param userDetail 领域实体
     * @return 视图对象
     */
    UserVo entity2Vo(User userDetail);
}
