package moe.byn.bynspring21.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;
import moe.byn.bynspring21.entity.RagLibrary;

public interface RagLibraryService extends IService<RagLibrary> {

    Page<RagLibrary> pageByUser(Integer pageNum, Integer pageSize);

    void addLibrary(RagLibrary library);

    void updateLibrary(RagLibrary library);

    void deleteWithDocs(String id);

    List<RagLibrary> listAllByUser();
}
