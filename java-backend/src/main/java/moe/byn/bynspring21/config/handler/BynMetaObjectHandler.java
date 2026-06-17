package moe.byn.bynspring21.config.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class BynMetaObjectHandler implements MetaObjectHandler {

    private final String DELETE      = "isDel";
    private final String CREATE_TIME = "createTime";
    private final String UPDATE_TIME = "updateTime";


    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, DELETE, Boolean.class, Boolean.FALSE);
        this.strictInsertFill(metaObject, CREATE_TIME, Date.class, new Date());
        this.strictInsertFill(metaObject, UPDATE_TIME, Date.class, new Date());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.setFieldValByName(UPDATE_TIME, new Date(), metaObject);
    }
}
