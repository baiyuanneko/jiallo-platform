package moe.byn.bynspring21.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import moe.byn.bynspring21.entity.RagLibraryDoc;

public interface RagLibraryDocService extends IService<RagLibraryDoc> {

    Page<RagLibraryDoc> pageByLibraryId(String libraryId, Integer pageNum, Integer pageSize);

    Page<RagLibraryDoc> pageByLibraryId(String libraryId, String keyword, Integer pageNum, Integer pageSize);

    void addDoc(RagLibraryDoc doc);

    void deleteDoc(String id);

    void renameDoc(String id, String newFileName);

    RagLibraryDoc getDocDetail(String id);
}
