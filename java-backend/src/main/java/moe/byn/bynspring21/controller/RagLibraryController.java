package moe.byn.bynspring21.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import moe.byn.bynspring21.entity.RagDocChunk;
import moe.byn.bynspring21.entity.User;
import moe.byn.bynspring21.entity.RagLibrary;
import moe.byn.bynspring21.entity.RagLibraryDoc;
import moe.byn.bynspring21.entity.base.R;
import moe.byn.bynspring21.entity.vo.RagLibrarySearchVo;
import moe.byn.bynspring21.entity.vo.RagLibrarySearchVo.SearchResultVo;
import moe.byn.bynspring21.entity.vo.RagLibraryVo;
import moe.byn.bynspring21.entity.vo.RagLibraryVo.DocPreviewVo;
import moe.byn.bynspring21.security.SecurityUtil;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import moe.byn.bynspring21.service.FeatureModuleAvailabilityService;
import moe.byn.bynspring21.service.RagDocChunkService;
import moe.byn.bynspring21.service.RagLibraryDocService;
import moe.byn.bynspring21.service.RagLibraryService;
import moe.byn.bynspring21.service.UserGroupMemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("ragLibrary")
@Tag(name = "RagLibrary", description = "RAG 知识库管理")
@Validated
public class RagLibraryController {

    @Autowired
    private RagLibraryService ragLibraryService;

    @Autowired
    private RagLibraryDocService ragLibraryDocService;

    @Autowired
    private RagDocChunkService ragDocChunkService;

    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private FeatureModuleAvailabilityService featureModuleAvailabilityService;

    @Autowired
    private UserGroupMemberService userGroupMemberService;

    @Operation(summary = "分页查询知识库", description = "分页查询当前用户的知识库列表", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("page")
    public R<Page<RagLibrary>> pageByUser(@Validated @RequestBody RagLibraryVo.PageRagLibraryVo vo) {
        return R.ok(ragLibraryService.pageByUser(vo.getPageNum(), vo.getPageSize()));
    }

    @Operation(summary = "查询全部知识库", description = "查询当前用户的所有知识库列表（不分页）", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("listAll")
    public R<List<RagLibrary>> listAll() {
        return R.ok(ragLibraryService.listAllByUser());
    }

    @Operation(summary = "新增知识库", description = "创建新的知识库", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("add")
    public R<Void> addLibrary(@Validated @RequestBody RagLibraryVo.AddRagLibraryVo vo) {
        RagLibrary library = RagLibrary.builder()
                .name(vo.getName())
                .description(vo.getDescription())
                .build();
        ragLibraryService.addLibrary(library);
        return R.ok();
    }

    @Operation(summary = "修改知识库", description = "修改知识库的名称和描述", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("update")
    public R<Void> updateLibrary(@Validated @RequestBody RagLibraryVo.UpdateRagLibraryVo vo) {
        RagLibrary library = new RagLibrary();
        library.setId(vo.getId());
        library.setName(vo.getName());
        library.setDescription(vo.getDescription());
        ragLibraryService.updateLibrary(library);
        return R.ok();
    }

    @Operation(summary = "删除知识库", description = "删除知识库及其所有文档", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("delete")
    public R<Void> deleteWithDocs(@Validated @RequestBody RagLibraryVo.DeleteVo vo) {
        ragLibraryService.deleteWithDocs(vo.getId());
        return R.ok();
    }

    @Operation(summary = "分页查询文档", description = "分页查询知识库中的文档列表", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("docs/page")
    public R<Page<RagLibraryDoc>> pageByLibraryId(@Validated @RequestBody RagLibraryVo.PageRagLibraryDocVo vo) {
        return R.ok(ragLibraryDocService.pageByLibraryId(vo.getLibraryId(), vo.getKeyword(), vo.getPageNum(), vo.getPageSize()));
    }

    @Operation(summary = "新增文档", description = "向知识库中添加文档并自动解析", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("docs/add")
    public R<Void> addDoc(@Validated @RequestBody RagLibraryVo.AddRagLibraryDocVo vo) {
        byte[] decodedBytes = Base64.getDecoder().decode(vo.getFileContent());
        String textContent = new String(decodedBytes, StandardCharsets.UTF_8);

        RagLibraryDoc doc = RagLibraryDoc.builder()
                .ragLibraryId(vo.getLibraryId())
                .fileName(vo.getFileName())
                .fileSize((long) decodedBytes.length)
                .build();
        ragLibraryDocService.addDoc(doc);

        ragDocChunkService.parseDocContent(doc.getId(), doc.getRagLibraryId(), textContent);
        return R.ok();
    }

    @Operation(summary = "删除文档", description = "删除知识库中的文档", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("docs/delete")
    public R<Void> deleteDoc(@Validated @RequestBody RagLibraryVo.DeleteDocVo vo) {
        ragLibraryDocService.deleteDoc(vo.getId());
        return R.ok();
    }

    @Operation(summary = "重命名文档", description = "修改文档的文件名", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("docs/rename")
    public R<Void> renameDoc(@Validated @RequestBody RagLibraryVo.RenameDocVo vo) {
        ragLibraryDocService.renameDoc(vo.getId(), vo.getFileName());
        return R.ok();
    }

    @Operation(summary = "获取文档预览", description = "获取文档元数据及从分块拼接的完整内容", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("docs/detail")
    public R<DocPreviewVo> getDocDetail(@Validated @RequestBody RagLibraryVo.DocDetailVo vo) {
        RagLibraryDoc doc = ragLibraryDocService.getDocDetail(vo.getId());
        List<RagDocChunk> chunks = ragDocChunkService.list(new LambdaQueryWrapper<RagDocChunk>()
                .eq(RagDocChunk::getDocId, vo.getId())
                .orderByAsc(RagDocChunk::getChunkIndex));
        String fullContent = chunks.stream()
                .map(RagDocChunk::getChunkContent)
                .collect(Collectors.joining());
        DocPreviewVo preview = DocPreviewVo.builder()
                .id(doc.getId())
                .fileName(doc.getFileName())
                .fileSize(doc.getFileSize())
                .content(fullContent)
                .build();
        return R.ok(preview);
    }

    @Operation(summary = "检索知识库", description = "在选中的知识库中全文检索，返回最匹配的分块", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("search")
    public R<List<SearchResultVo>> search(@Validated @RequestBody RagLibrarySearchVo.SearchVo vo) {
        return R.ok(ragDocChunkService.searchChunks(vo.getLibraryIds(), vo.getQuery(), vo.getLimit()));
    }

    @Operation(summary = "检查 RAG 知识库授权", description = "当前用户是否有 RAG 知识库功能授权", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("checkAuth")
    public R<Boolean> checkAuth() {
        User currentUser = securityUtil.getUser();
        List<String> groupIds = userGroupMemberService.getUserGroupIds(currentUser.getId());
        boolean authorized = featureModuleAvailabilityService.isModuleAvailable("rag_knowledge_base", currentUser.getRoleType(), groupIds);
        return R.ok(authorized);
    }
}
