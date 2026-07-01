package com.xx9527.fw.plus.boot.mate.sample.controller;

//import brave.Tracer;
import cn.afterturn.easypoi.excel.ExcelImportUtil;
import cn.afterturn.easypoi.excel.entity.FwPlusExportParams;
import cn.afterturn.easypoi.excel.entity.FwPlusImportParams;
import cn.afterturn.easypoi.excel.entity.ImportParams;
import cn.afterturn.easypoi.excel.entity.result.ExcelImportResult;
import cn.afterturn.easypoi.handler.inter.IExcelExportServer;
import cn.afterturn.easypoi.handler.inter.IReadHandler;
import cn.afterturn.easypoi.util.TransUtils;
import cn.afterturn.easypoi.word.parse.ParseWord07;
import com.fhs.core.trans.anno.TransMethodResult;
import com.fhs.trans.service.impl.DictionaryTransService;
import com.google.common.collect.Lists;
import com.xx9527.fw.plus.common.mate.annotation.FieldIgnoreSensitive;
import com.xx9527.fw.plus.boot.mate.sample.dto.form.ImportUserForm;
import com.xx9527.fw.plus.boot.mate.sample.dto.form.SaveUserForm;
import com.xx9527.fw.plus.boot.mate.sample.dto.form.TestForm;
import com.xx9527.fw.plus.boot.mate.sample.dto.vo.UserExtInfo;
import com.xx9527.fw.plus.boot.mate.sample.dto.vo.UserInfoVo;
import com.xx9527.fw.plus.common.validator.CommonsValidator;
import com.xx9527.fw.plus.easytrans.annotation.TransMethodArg;
import lombok.extern.slf4j.Slf4j;
import cn.afterturn.easypoi.excel.export.styler.DefaultExcelExportStyle;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import cn.afterturn.easypoi.entity.vo.BigExcelConstants;
import cn.afterturn.easypoi.excel.entity.enmus.ExcelType;
import cn.afterturn.easypoi.view.PoiBaseView;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.*;

@FieldIgnoreSensitive(values = {"address", "mobile"})
@RestController
@Slf4j
@RequestMapping("/test")
public class TestController {

    @Autowired
    private DictionaryTransService dictionaryTransService;

    @GetMapping("/exportWord")
    public void exportWord(HttpServletResponse response) throws Exception {
        Map<String, Object> dicMap = new HashMap<>();
        dicMap.put("bloodSugar", "张三=========");
        //dicMap.put("imgUrl", "https://img2.woyaogexing.com/2024/05/22/7e07e8919864240a!400x400.jpg");
        ParseWord07 parseWord07 = new ParseWord07();
        XWPFDocument document = parseWord07.parseWord("1234567890.docx", dicMap);

        // 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        response.setHeader("Content-Disposition", "attachment; filename=document.docx");

        // 将文档写入响应输出流
        document.write(response.getOutputStream());
        response.getOutputStream().flush();
        response.getOutputStream().close();
        document.close();
    }

    @TransMethodResult
    @GetMapping("/userExtInfo")
    public UserExtInfo userExtInfo() {
        UserExtInfo userExtInfo = new UserExtInfo();
        userExtInfo.setId(1L);
        userExtInfo.setDesc("我是描述");
        return userExtInfo;
    }

    @PostMapping("/mytest")
    public String mytest(@Valid @RequestBody TestForm form) {
        //手动校验
        CommonsValidator.validate2MethodArgumentNotValidException(form, TestForm.TestFormGroup.class);
        return "hello";
    }

    @PostMapping("/importExcelBySax")
    public Object importExcelBySax(@RequestPart(value = "file", required = true) MultipartFile file) throws Exception {
        ImportParams params = new ImportParams();
        params.setNeedVerify(true);
        List<ImportUserForm> list = new ArrayList<>();
        ExcelImportUtil.importExcelBySax(file.getInputStream(), ImportUserForm.class, params,
                new IReadHandler<ImportUserForm>(){
                    @Override
                    public void handler(ImportUserForm o) {
                        TransUtils.transObject(o, null, null);
                        list.add(o);
                    }
                    @Override
                    public void doAfterAll() {
                    }
                });
        return list;
    }

    @PostMapping("/importData")
    public Object importData(@RequestPart(value = "file", required = true) MultipartFile file) throws Exception {
        FwPlusImportParams params = new FwPlusImportParams();
        params.setNeedVerify(true);
        params.setNeedTrans(Boolean.TRUE);
        ExcelImportResult<ImportUserForm> result = ExcelImportUtil.importExcelMore(file.getInputStream(), ImportUserForm.class, params);
        List<ImportUserForm> list = new ArrayList<>();
        if (ObjectUtils.isNotEmpty(result.getList())) {
            list.addAll(result.getList());
        }
        if (ObjectUtils.isNotEmpty(result.getFailList())) {
            list.addAll(result.getFailList());
        }
        return list;
    }

    @PostMapping(value = "/exportData", produces = "application/octet-stream")
    public void exportData(HttpServletResponse response, HttpServletRequest request) {
        ModelMap map = new ModelMap();
        FwPlusExportParams params = new FwPlusExportParams(null, "用户列表", ExcelType.HSSF);
        params.setNeedTrans(Boolean.TRUE);
        params.setStyle(DefaultExcelExportStyle.class);
        map.put(BigExcelConstants.CLASS, UserInfoVo.class);
        map.put(BigExcelConstants.PARAMS, params);
        // 查询参数
        //map.put(BigExcelConstants.DATA_PARAMS, form);
        map.put(BigExcelConstants.DATA_INTER, new IExcelExportServer(){
            @Override
            public List<Object> selectListForExcelExport(Object o, int i) {
                log.info("开始导出第{}页", i);
                if (i > 1) {
                    return Lists.newArrayList();
                }
                UserInfoVo userInfo = new UserInfoVo();
                userInfo.setMobile("18579062761");
                userInfo.setAddress("江西省南昌市新建区罗亭镇详细建昌大道77号沿街商铺");
                userInfo.setEmail("xiongxiao@jg-tech.com");
                userInfo.setCustom("你好");
                userInfo.setUserExtId(1L);
                userInfo.setSex(1);
                return Lists.newArrayList(userInfo);
            }
        });
        map.put(BigExcelConstants.FILE_NAME, "用户列表");
        PoiBaseView.render(map, request, response, BigExcelConstants.EASYPOI_BIG_EXCEL_VIEW);
    }


    /**
    //@TransDefaultSett
    @TransMethodArg
    //@TransMethodResult
    @PostMapping("/saveUser")
    public RestResult<UserInfoVo> saveUser(@Valid @RequestBody SaveUserForm saveUserForm) {
        UserInfoVo userInfo = new UserInfoVo();
        userInfo.setMobile("18579062761");
        //userInfo.setAddress("江西省南昌市新建区罗亭镇详细建昌大道77号沿街商铺");
        userInfo.setAddress("江西省南昌市新建区罗亭镇详细建昌大道77号沿街商铺");
        userInfo.setEmail("xiongxiao@jg-tech.com");
        userInfo.setCustom("你好");
        userInfo.setUserExtId(1L);
        userInfo.setSex(1);
        return RestResult.ok().data(userInfo);
    }


    @TransMethodResult
    @GetMapping("/userInfo")
    public RestResult<UserInfoVo> userInfo() {
        UserInfoVo userInfo = new UserInfoVo();
        userInfo.setMobile("18579062761");
        //userInfo.setAddress("江西省南昌市新建区罗亭镇详细建昌大道77号沿街商铺");
        userInfo.setAddress("江西省南昌市新建区罗亭镇详细建昌大道77号沿街商铺");
        userInfo.setEmail("xiongxiao@jg-tech.com");
        userInfo.setCustom("你好");
        userInfo.setUserExtId(1L);
        userInfo.setSex(1);
        userInfo.setDate(new Date());
        userInfo.setDelFlag(Boolean.TRUE);
        return RestResult.ok().data(userInfo);
    }

     **/


    @GetMapping("/userInfoList")
    public List<UserInfoVo> userInfoList() {
        UserInfoVo userInfo = new UserInfoVo();
        userInfo.setMobile("18579062761");
        //userInfo.setAddress("江西省南昌市新建区罗亭镇详细建昌大道77号沿街商铺");
        userInfo.setAddress("江西省南昌市新建区罗亭镇详细建昌大道77号沿街商铺");
        userInfo.setEmail("xiongxiao@jg-tech.com");
        userInfo.setCustom("你好");

        List<UserInfoVo> list = new ArrayList<>();
        list.add(userInfo);
        return list;
    }

    @GetMapping("/userInfoList2")
    public List<UserInfoVo> userInfoList2() {
        UserInfoVo userInfo = new UserInfoVo();
        userInfo.setMobile("18579062761");
        //userInfo.setAddress("江西省南昌市新建区罗亭镇详细建昌大道77号沿街商铺");
        userInfo.setAddress("江西省南昌市新建区罗亭镇详细建昌大道77号沿街商铺");
        userInfo.setEmail("xiongxiao@jg-tech.com");
        userInfo.setCustom("你好");
        List<UserInfoVo> list = new ArrayList<>();
        list.add(userInfo);
        return list;
    }


    @GetMapping("/userInfoList1")
    public List<UserInfoVo> userInfoList1() {
        UserInfoVo userInfo = new UserInfoVo();
        userInfo.setMobile("18579062761");
        //userInfo.setAddress("江西省南昌市新建区罗亭镇详细建昌大道77号沿街商铺");
        userInfo.setAddress("江西省南昌市新建区罗亭镇详细建昌大道77号沿街商铺");
        userInfo.setEmail("xiongxiao@jg-tech.com");
        userInfo.setCustom("你好");

        List<UserInfoVo> list = new ArrayList<>();
        list.add(userInfo);
        return list;
    }
}
