package moe.byn.bynspring21.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import moe.byn.bynspring21.entity.RagLibrary;
import moe.byn.bynspring21.exception.BynBaseException;
import moe.byn.bynspring21.mapper.RagLibraryMapper;
import moe.byn.bynspring21.security.SecurityUtil;
import moe.byn.bynspring21.service.RagLibraryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class RagLibraryServiceImpl extends ServiceImpl<RagLibraryMapper, RagLibrary> implements RagLibraryService {

    @Autowired
    private SecurityUtil securityUtil;

    @Override
    public Page<RagLibrary> pageByUser(Integer pageNum, Integer pageSize) {
        Page<RagLibrary> page = new Page<>(pageNum, pageSize);
        return this.page(page, new LambdaQueryWrapper<RagLibrary>()
                .eq(RagLibrary::getCreateUser, securityUtil.getUserId())
                .orderByDesc(RagLibrary::getCreateTime));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addLibrary(RagLibrary library) {
        library.setCreateUser(securityUtil.getUserId());
        this.save(library);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLibrary(RagLibrary library) {
        RagLibrary existing = this.getById(library.getId());
        if (existing == null) {
            throw new BynBaseException("知识库不存在");
        }
        if (!existing.getCreateUser().equals(securityUtil.getUserId())) {
            throw new BynBaseException("无权修改此知识库");
        }
        // 只更新 name 和 description，避免覆盖其他字段（如 totalFileSize、docCount）
        this.update(new LambdaUpdateWrapper<RagLibrary>()
                .set(RagLibrary::getName, library.getName())
                .set(RagLibrary::getDescription, library.getDescription())
                .eq(RagLibrary::getId, library.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWithDocs(String id) {
        RagLibrary library = this.getById(id);
        if (library == null) {
            throw new BynBaseException("知识库不存在");
        }
        if (!library.getCreateUser().equals(securityUtil.getUserId())) {
            throw new BynBaseException("无权删除此知识库");
        }
        if (library.getDocCount() > 0) {
            throw new BynBaseException("请先删除所有文档后再删除知识库");
        }
        this.removeById(id);
    }

    @Override
    public List<RagLibrary> listAllByUser() {
        return this.list(new LambdaQueryWrapper<RagLibrary>()
                .eq(RagLibrary::getCreateUser, securityUtil.getUserId())
                .orderByDesc(RagLibrary::getCreateTime));
    }
}
