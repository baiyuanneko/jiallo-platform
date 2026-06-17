package moe.byn.bynspring21.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import moe.byn.bynspring21.entity.RagLibrary;
import moe.byn.bynspring21.entity.RagLibraryDoc;
import moe.byn.bynspring21.exception.BynBaseException;
import moe.byn.bynspring21.mapper.RagLibraryDocMapper;
import moe.byn.bynspring21.mapper.RagLibraryMapper;
import moe.byn.bynspring21.security.SecurityUtil;
import moe.byn.bynspring21.service.RagDocChunkService;
import moe.byn.bynspring21.service.RagLibraryDocService;
import moe.byn.bynspring21.service.RagLibraryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class RagLibraryDocServiceImpl extends ServiceImpl<RagLibraryDocMapper, RagLibraryDoc> implements RagLibraryDocService {

    private static final long MAX_USER_TOTAL_BYTES = 64L * 1024 * 1024;

    @Autowired
    @Lazy
    private RagLibraryService ragLibraryService;

    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private RagLibraryMapper ragLibraryMapper;

    @Autowired
    private RagDocChunkService ragDocChunkService;

    @Override
    public Page<RagLibraryDoc> pageByLibraryId(String libraryId, Integer pageNum, Integer pageSize) {
        return pageByLibraryId(libraryId, null, pageNum, pageSize);
    }

    @Override
    public Page<RagLibraryDoc> pageByLibraryId(String libraryId, String keyword, Integer pageNum, Integer pageSize) {
        RagLibrary library = ragLibraryService.getById(libraryId);
        if (library == null) {
            throw new BynBaseException("知识库不存在");
        }
        if (!library.getCreateUser().equals(securityUtil.getUserId())) {
            throw new BynBaseException("无权查看此知识库的文档");
        }
        Page<RagLibraryDoc> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<RagLibraryDoc> wrapper = new LambdaQueryWrapper<RagLibraryDoc>()
                .eq(RagLibraryDoc::getRagLibraryId, libraryId);
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.like(RagLibraryDoc::getFileName, keyword);
        }
        wrapper.orderByDesc(RagLibraryDoc::getCreateTime);
        return this.page(page, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addDoc(RagLibraryDoc doc) {
        RagLibrary library = ragLibraryService.getById(doc.getRagLibraryId());
        if (library == null) {
            throw new BynBaseException("知识库不存在");
        }
        if (!library.getCreateUser().equals(securityUtil.getUserId())) {
            throw new BynBaseException("无权向此知识库添加文档");
        }

        // 检查用户总文件大小限制
        long currentTotal = ragLibraryMapper.sumFileSizeByUser(securityUtil.getUserId());
        if (currentTotal + doc.getFileSize() > MAX_USER_TOTAL_BYTES) {
            throw new BynBaseException("所有知识库总文件大小已超过 128MB 限制");
        }

        doc.setCreateUser(securityUtil.getUserId());
        this.save(doc);
        ragLibraryService.update()
                .setSql("doc_count = doc_count + 1, total_file_size = total_file_size + " + doc.getFileSize())
                .eq("id", doc.getRagLibraryId())
                .update();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDoc(String id) {
        RagLibraryDoc doc = this.getById(id);
        if (doc == null) {
            throw new BynBaseException("文档不存在");
        }
        RagLibrary library = ragLibraryService.getById(doc.getRagLibraryId());
        if (!library.getCreateUser().equals(securityUtil.getUserId())) {
            throw new BynBaseException("无权删除此文档");
        }
        long fileSize = doc.getFileSize() != null ? doc.getFileSize() : 0L;
        this.removeById(id);
        ragLibraryService.update()
                .setSql("doc_count = doc_count - 1, total_file_size = GREATEST(total_file_size - " + fileSize + ", 0)")
                .eq("id", doc.getRagLibraryId())
                .update();
        ragDocChunkService.deleteByDocId(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void renameDoc(String id, String newFileName) {
        RagLibraryDoc doc = this.getById(id);
        if (doc == null) {
            throw new BynBaseException("文档不存在");
        }
        RagLibrary library = ragLibraryService.getById(doc.getRagLibraryId());
        if (!library.getCreateUser().equals(securityUtil.getUserId())) {
            throw new BynBaseException("无权修改此文档");
        }
        doc.setFileName(newFileName);
        this.updateById(doc);
    }

    @Override
    public RagLibraryDoc getDocDetail(String id) {
        RagLibraryDoc doc = this.getById(id);
        if (doc == null) {
            throw new BynBaseException("文档不存在");
        }
        RagLibrary library = ragLibraryService.getById(doc.getRagLibraryId());
        if (!library.getCreateUser().equals(securityUtil.getUserId())) {
            throw new BynBaseException("无权查看此文档");
        }
        return doc;
    }
}
