package cn.afterturn.easypoi.wps.controller;

import cn.afterturn.easypoi.wps.entity.WpsToken;
import cn.afterturn.easypoi.wps.entity.resreq.*;
import cn.afterturn.easypoi.wps.service.IEasyPoiWpsViewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Map;

/**
 * @author jueyue on 20-5-7.
 */
@Slf4j
@RestController
@RequestMapping("easypoi/wps/v1/3rd/file")
public class EasyPoiFileController {

    /**
     * 自动注入IEasyPoiWpsViewService服务接口实例
     * 该服务用于处理EasyPoi与WPS相关的视图操作
     * required = false表示该依赖注入不是必须的，如果容器中不存在对应的bean也不会抛出异常
     */
    @Autowired(required = false)
    private IEasyPoiWpsViewService easyPoiWpsService;


    /**
     * 获取网络文件预览URL
     *
     * @param filePath 文件路径
     * @return 预览URL对应的token信息
     */
    @GetMapping("getViewUrl")
    public WpsToken getViewUrl(String filePath) {
        // 记录获取预览URL的日志信息
        log.info("getViewUrlWebPath：filePath={}", filePath);
        // 调用服务获取文件预览URL
        WpsToken token = easyPoiWpsService.getViewUrl(decode(filePath));
        return token;
    }


    /**
     * 获取文件元数据
     *
     * @param request HTTP请求对象，用于获取请求头信息
     * @param userId 用户ID，可选参数，用于标识用户身份
     * @param filePath 文件路径，必填参数，指定要获取元数据的文件路径
     * @return WpsFileResponse 文件元数据响应对象，包含文件的基本信息
     */
    @GetMapping("info")
    public WpsFileResponse getFileInfo(HttpServletRequest request, @RequestParam(value = "_w_userid", required = false) String userId,
                                       @RequestParam("_w_filepath") String filePath) {
        log.info("获取文件元数据userId:{},path:{}", userId, filePath);
        // 从请求头中获取文件ID
        String          fileId       = request.getHeader("x-weboffice-file-id");
        // 调用服务获取文件信息
        WpsFileResponse fileResponse = easyPoiWpsService.getFileInfo(userId, decode(filePath), fileId);
        return fileResponse;
    }


    /**
     * 通知此文件目前有哪些人正在协作
     * @param request HTTP请求对象，用于获取文件ID等请求头信息
     * @param list WpsUserRequest对象，包含当前协作用户的列表信息
     * @return WpsResponse对象，返回操作结果状态
     */
    @PostMapping("online")
    public WpsResponse fileOnline(HttpServletRequest request, @RequestBody WpsUserRequest list) {
        // 记录协作用户通知日志
        log.info("通知此文件目前有哪些人正在协作param:{}", list);
        // 从请求头中获取文件ID
        String fileId = request.getHeader("x-weboffice-file-id");
        // 调用服务层处理文件协作用户信息
        easyPoiWpsService.fileOnline(fileId, list);
        return WpsResponse.success();
    }


    /**
     * 上传文件新版本
     *
     * @param request HTTP请求对象，用于获取请求头信息
     * @param userId 用户ID，可选参数
     * @param file 上传的文件数据
     * @return WpsFileSaveResponse 文件保存响应结果
     */
    @PostMapping("save")
    public WpsFileSaveResponse fileSave(HttpServletRequest request,
                                        @RequestParam(value = "_w_userid", required = false) String userId,
                                        @RequestBody MultipartFile file) {
        log.info("上传文件新版本");
        // 从请求头中获取文件ID
        String fileId = request.getHeader("x-weboffice-file-id");
        return new WpsFileSaveResponse(easyPoiWpsService.fileSave(fileId, userId, file));
    }


    /**
     * 获取特定版本的文件信息
     *
     * @param request HTTP请求对象，用于获取文件ID等请求头信息
     * @param version 文件版本号
     * @param filePath 文件路径参数
     * @return 返回指定版本的文件信息响应对象
     */
    @GetMapping("version/{version}")
    public WpsFileSaveResponse fileVersion(HttpServletRequest request, @PathVariable Integer version,
                                           @RequestParam("_w_filepath") String filePath) {
        log.info("获取特定版本的文件信息version:{}", version);
        // 从请求头中获取文件ID
        String fileId = request.getHeader("x-weboffice-file-id");
        // 调用服务获取指定版本的文件信息并返回响应
        return new WpsFileSaveResponse(easyPoiWpsService.getFileInfoOfVersion(fileId, decode(filePath), version));
    }


    /**
     * 文件重命名
     *
     * @param request HTTP请求对象，用于获取文件ID等请求头信息
     * @param req 重命名请求参数对象，包含新的文件名
     * @param userId 用户ID，可选参数
     * @return WpsResponse 重命名操作结果响应
     */
    @PutMapping("rename")
    public WpsResponse fileRename(HttpServletRequest request, @RequestBody WpsRenameRequest req,
                                  @RequestParam(value = "_w_userid", required = false) String userId) {
        log.info("文件重命名param:{},userId:{}", req, userId);
        // 从请求头中获取文件ID
        String fileId = request.getHeader("x-weboffice-file-id");
        // 调用服务层执行文件重命名操作
        easyPoiWpsService.rename(fileId, userId, req.getName());
        return WpsResponse.success();
    }


    /**
     * 获取所有历史版本文件信息
     * @param req 文件历史请求参数对象，包含查询历史版本所需的信息
     * @return WpsFileHistoryResponse 文件历史版本响应对象，包含历史版本文件列表信息
     */
    @PostMapping("history")
    public WpsFileHistoryResponse fileHistory(@RequestBody WpsFileHistoryRequest req) {
        log.info("获取所有历史版本文件信息param:{}", req);
        return easyPoiWpsService.getHistory(req);
    }


    /**
     * 新建文件
     *
     * @param file 上传的文件对象
     * @param userId 用户ID，可选参数
     * @return WpsResponse 响应结果，表示操作成功
     */
    @PostMapping("new")
    public WpsResponse fileNew(@RequestBody MultipartFile file, @RequestParam(value = "_w_userid", required = false) String userId) {
        // 记录新建文件操作日志
        log.info("新建文件,只打印不实现,userid:{}", userId);
        return WpsResponse.success();
    }


    /**
     * 回调通知
     * 处理WPS回调通知请求，仅记录日志不执行具体业务逻辑
     * @param obj 回调请求参数对象
     * @return WpsResponse 统一响应结果，返回成功状态
     */
    @PostMapping("onnotify")
    public WpsResponse onNotify(@RequestBody Map obj) {
        // 记录回调通知参数信息
        log.info("回调通知,只打印不实现,param:{}", obj);
        return WpsResponse.success();
    }


    /**
     * 对文件路径进行URL解码
     *
     * @param filePath 需要解码的文件路径字符串
     * @return 解码后的文件路径，如果解码失败则返回原始路径
     */
    public String decode(String filePath) {
        try {
            // 使用UTF-8编码对文件路径进行URL解码
            filePath = URLDecoder.decode(filePath, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            log.error(e.getMessage(), e);
        }
        return filePath;
    }


}
