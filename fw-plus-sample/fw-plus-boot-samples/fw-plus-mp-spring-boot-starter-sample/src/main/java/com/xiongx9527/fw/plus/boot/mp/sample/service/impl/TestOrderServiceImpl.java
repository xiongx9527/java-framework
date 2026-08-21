package com.xiongx9527.fw.plus.boot.mp.sample.service.impl;

import cn.afterturn.easypoi.excel.entity.result.ExcelImportResult;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.xiongx9527.fw.plus.boot.mp.sample.controller.TestOrderController;
import com.xiongx9527.fw.plus.boot.mp.sample.entity.*;
import com.xiongx9527.fw.plus.boot.mp.sample.mapper.*;
import com.xiongx9527.fw.plus.boot.mp.sample.service.TestOrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.util.Lists;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TestOrderServiceImpl implements TestOrderService {

    @Resource
    TestOrderMapper testOrderMapper;

    @Resource
    TestOrderItemMapper orderItemMapper;

    @Override
    public void test01() {
        java.util.List<TestOrderEntity> list = testOrderMapper.selectList(Wrappers.lambdaQuery(TestOrderEntity.class));
        log.info("list:{}", JSON.toJSONString(list));
    }

    @Override
    public void test02() {
        TestOrderEntity testOrder = testOrderMapper.selectById(1L);
        log.info("testOrder:{}", JSON.toJSONString(testOrder));
        testOrder.setStatus(2);
        //testOrderMapper.updateById(testOrder);

        new LambdaUpdateChainWrapper<>(testOrderMapper)
                .eq(TestOrderEntity::getId, 1L)
                .set(TestOrderEntity::getStatus, 9)
                .update();
    }

    @Override
    public void test03() {
        java.util.List<TestOrderEntity> list = testOrderMapper.selectBatchIds(Lists.list(1L,2L));
        log.info("list:{}", JSON.toJSONString(list));
        list.stream().forEach(e->{
            e.setStatus(5);
        });
        //testOrderMapper.batchUpdateByIds(list);
    }

    @Override
    public void test04() {
        MPJLambdaWrapper<TestOrderItemEntity> wrapper = new MPJLambdaWrapper<TestOrderItemEntity>()
                .leftJoin(TestOrderEntity.class, TestOrderEntity::getId, TestOrderItemEntity::getOrderId);
        java.util.List<TestOrderItemEntity> list = orderItemMapper.selectJoinList(wrapper);
        log.info("list:{}", JSON.toJSONString(list));
    }

    @Override
    public void test05() {
        TestOrderEntity testOrder = testOrderMapper.selectById(1L);
        testOrderMapper.deleteById(1L);
    }

    @Override
    public Long test06() {
        TestOrderEntity testOrder = new TestOrderEntity();
        testOrder.setName("test06");
        testOrder.setStatus(1);
        testOrder.setPhone("12345678901");
        testOrderMapper.insert(testOrder);
        Long id = (Long) testOrder.getId();
        TestOrderEntity oldtestOrder = testOrderMapper.selectById(id);
        return id;
    }

    @Resource
    EvaluationSubjectMapper evaluationSubjectMapper;

    @Resource
    EvaluationSubjectQuestionBodyMapper evaluationSubjectQuestionBodyMapper;

    @Resource
    EvaluationSubjectQuestionGroupMapper evaluationSubjectQuestionGroupMapper;

    @Resource
    EvaluationQuestionBodyMapper evaluationQuestionBodyMapper;

    @Resource
    EvaluationQuestionBodySelectorMapper evaluationQuestionBodySelectorMapper;

    @Resource
    EvaluationTemplateMapper evaluationTemplateMapper;

    @Resource
    EvaluationTemplateQuestionBodyMapper evaluationTemplateQuestionBodyMapper;

    @Resource
    EvaluationTemplateQuestionBodySelectorMapper evaluationTemplateQuestionBodySelectorMapper;

    /**
     * 批量导入
     * @param result
     */
    @Transactional(rollbackFor = Throwable.class)
    @Override
    public void test07(ExcelImportResult<TestOrderController.ImportInfoForm> result,int startSheetIndex) {
        String sheetName = result.getWorkbook().getSheetAt(startSheetIndex).getSheetName();
        String[] sheetNames = sheetName.split("-");
        String id = sheetNames[0];
        String code = sheetNames[1];
        String name = sheetNames[2];

        EvaluationSubjectEntity evaluationSubjectEntity = evaluationSubjectMapper.selectById(id);
        if (evaluationSubjectEntity == null) {
            evaluationSubjectEntity = new EvaluationSubjectEntity();
            evaluationSubjectEntity.setCode(code);
            evaluationSubjectEntity.setName(name);
            evaluationSubjectEntity.setId(Long.parseLong(id));
            evaluationSubjectEntity.setDeleteFlag(false);
            evaluationSubjectMapper.insert(evaluationSubjectEntity);
        } else if (evaluationSubjectEntity.getDeleteFlag()) {
            evaluationSubjectEntity.setName(name);
            evaluationSubjectMapper.updateById(evaluationSubjectEntity);
        }
        if (!code.equalsIgnoreCase(evaluationSubjectEntity.getCode())) {
            throw new RuntimeException("科目编号不一致");
        }

        //科目下的题目
        List<EvaluationSubjectQuestionBodyEntity> list = ListUtils.emptyIfNull(evaluationSubjectQuestionBodyMapper.selectList(Wrappers.lambdaQuery(EvaluationSubjectQuestionBodyEntity.class)
                .eq(EvaluationSubjectQuestionBodyEntity::getEvaluationSubjectId, id)));
        //转成map
        Map<Long, EvaluationSubjectQuestionBodyEntity> evaluationSubjectQuestionBodyEntityMap = list.stream().collect(Collectors.toMap(e->{
            return Long.parseLong(e.getId().toString());
        }, e -> e));

        //题干ID
        List<Long> evaluationQuestionBodyIdList = list.stream().map(e ->e.getEvaluationQuestionBodyId()).collect(Collectors.toList());
        List<EvaluationQuestionBodyEntity> evaluationQuestionBodyEntityList = new ArrayList<>();
        if  (CollectionUtils.isNotEmpty(evaluationQuestionBodyIdList)) {
            evaluationQuestionBodyEntityList = evaluationQuestionBodyMapper.selectBatchIds(evaluationQuestionBodyIdList);
        }
        //转成map
        Map<Long, EvaluationQuestionBodyEntity> evaluationQuestionBodyEntityMap = evaluationQuestionBodyEntityList.stream().collect(Collectors.toMap(e->{
            return Long.parseLong(e.getId().toString());
        }, e -> e));

        //题干的选项
        List<EvaluationQuestionBodySelectorEntity> evaluationQuestionBodySelectorEntityList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(evaluationQuestionBodyIdList)) {
            evaluationQuestionBodySelectorEntityList = evaluationQuestionBodySelectorMapper.selectList(Wrappers.lambdaQuery(EvaluationQuestionBodySelectorEntity.class)
                    .in(EvaluationQuestionBodySelectorEntity::getEvaluationQuestionBodyId, evaluationQuestionBodyIdList));
        }
        //转成map
        Map<Long, EvaluationQuestionBodySelectorEntity> evaluationQuestionBodySelectorEntityMap = evaluationQuestionBodySelectorEntityList.stream().collect(Collectors.toMap(e->{
            return Long.parseLong(e.getId().toString());
        }, e -> e));

        //题目分组ID
        //List<Long> evaluationSubjectQuestionGroupIdList = list.stream().map(e ->e.getEvaluationSubjectQuestionGroupId()).collect(Collectors.toList());
        List<EvaluationSubjectQuestionGroupEntity>  evaluationSubjectQuestionGroupEntityList =
                evaluationSubjectQuestionGroupMapper.selectList(Wrappers.lambdaQuery(EvaluationSubjectQuestionGroupEntity.class)
                        .eq(EvaluationSubjectQuestionGroupEntity::getEvaluationSubjectId, id));
        //转成map
        Map<Long, EvaluationSubjectQuestionGroupEntity> evaluationSubjectQuestionGroupEntityMap = evaluationSubjectQuestionGroupEntityList.stream().collect(Collectors.toMap(e->{
            return Long.parseLong(e.getId().toString());
        }, e -> e));

        //科目
        EvaluationSubjectEntity finalEvaluationSubjectEntity = evaluationSubjectEntity;
        result.getList().stream().forEach(e->{
            String itemid = e.getId();
            log.info("id:{}",  itemid);
            //分组
            if (itemid.endsWith("00000000") && !itemid.endsWith("000000000")) {
                EvaluationSubjectQuestionGroupEntity evaluationSubjectQuestionGroupEntity = evaluationSubjectQuestionGroupEntityMap.get(Long.parseLong(itemid));
                if (evaluationSubjectQuestionGroupEntity == null) {
                    evaluationSubjectQuestionGroupEntity = new EvaluationSubjectQuestionGroupEntity();
                    evaluationSubjectQuestionGroupEntity.setCode(e.getCode());
                    evaluationSubjectQuestionGroupEntity.setName(e.getName());
                    evaluationSubjectQuestionGroupEntity.setId(Long.parseLong(itemid));
                    evaluationSubjectQuestionGroupEntity.setEvaluationSubjectId(Long.parseLong(finalEvaluationSubjectEntity.getId().toString()));
                    evaluationSubjectQuestionGroupEntity.setDeleteFlag(false);
                    evaluationSubjectQuestionGroupMapper.insert(evaluationSubjectQuestionGroupEntity);
                } else {
                    evaluationSubjectQuestionGroupEntity.setCode(e.getCode());
                    evaluationSubjectQuestionGroupEntity.setName(e.getName());
                    evaluationSubjectQuestionGroupEntity.setEvaluationSubjectId(Long.parseLong(finalEvaluationSubjectEntity.getId().toString()));
                    evaluationSubjectQuestionGroupMapper.updateById(evaluationSubjectQuestionGroupEntity);
                }
            } else {
                //题干
                if (!StringUtils.isEmpty(e.getType())) {
                    EvaluationQuestionBodyEntity evaluationQuestionBodyEntity = evaluationQuestionBodyEntityMap.get(Long.parseLong(itemid));
                    if (evaluationQuestionBodyEntity == null) {
                        evaluationQuestionBodyEntity = new EvaluationQuestionBodyEntity();
                        evaluationQuestionBodyEntity.setId(Long.parseLong(itemid));
                        evaluationQuestionBodyEntity.setCode(e.getCode());
                        evaluationQuestionBodyEntity.setName(e.getName());
                        evaluationQuestionBodyEntity.setType(Integer.parseInt(e.getType()));
                        evaluationQuestionBodyEntity.setJsonPath(e.getJsonPath());
                        evaluationQuestionBodyEntity.setDeleteFlag(false);
                        if (!StringUtils.isEmpty(e.getMultiSelectFlag())) {
                            evaluationQuestionBodyEntity.setMultiSelectFlag(e.getMultiSelectFlag().equals("1"));
                        }
                        if (!StringUtils.isEmpty(e.getParentBodyId())) {
                            evaluationQuestionBodyEntity.setParentId(Long.parseLong(e.getParentBodyId()));
                        }
                        if (!StringUtils.isEmpty(e.getNextBodyId())) {
                            evaluationQuestionBodyEntity.setNextEvaluationQuestionBodyId(Long.parseLong(e.getNextBodyId()));
                        }
                        if (!StringUtils.isEmpty(e.getOrderIndex())) {
                            evaluationQuestionBodyEntity.setSort(Long.parseLong(e.getOrderIndex()));
                        } else {
                            evaluationQuestionBodyEntity.setSort(Long.parseLong(evaluationQuestionBodyEntity.getId().toString()));
                        }
                        evaluationQuestionBodyMapper.insert(evaluationQuestionBodyEntity);
                    } else {
                        evaluationQuestionBodyEntity.setName(e.getName());
                        evaluationQuestionBodyEntity.setCode(e.getCode());
                        evaluationQuestionBodyEntity.setType(Integer.parseInt(e.getType()));
                        evaluationQuestionBodyEntity.setJsonPath(e.getJsonPath());
                        if (!StringUtils.isEmpty(e.getMultiSelectFlag())) {
                            evaluationQuestionBodyEntity.setMultiSelectFlag(e.getMultiSelectFlag().equals("1"));
                        }
                        if (!StringUtils.isEmpty(e.getParentBodyId())) {
                            evaluationQuestionBodyEntity.setParentId(Long.parseLong(e.getParentBodyId()));
                        }
                        if (!StringUtils.isEmpty(e.getNextBodyId())) {
                            evaluationQuestionBodyEntity.setNextEvaluationQuestionBodyId(Long.parseLong(e.getNextBodyId()));
                        }
                        if (!StringUtils.isEmpty(e.getOrderIndex())) {
                            evaluationQuestionBodyEntity.setSort(Long.parseLong(e.getOrderIndex()));
                        } else {
                            evaluationQuestionBodyEntity.setSort(Long.parseLong(evaluationQuestionBodyEntity.getId().toString()));
                        }
                        evaluationQuestionBodyMapper.updateById(evaluationQuestionBodyEntity);
                    }
                    EvaluationSubjectQuestionBodyEntity evaluationSubjectQuestionBodyEntity = evaluationSubjectQuestionBodyEntityMap.get(evaluationQuestionBodyEntity.getId());
                    if (evaluationSubjectQuestionBodyEntity == null) {
                        evaluationSubjectQuestionBodyEntity = new EvaluationSubjectQuestionBodyEntity();
                        evaluationSubjectQuestionBodyEntity.setId(evaluationQuestionBodyEntity.getId());
                        evaluationSubjectQuestionBodyEntity.setDeleteFlag(false);
                        evaluationSubjectQuestionBodyEntity.setEvaluationSubjectId(Long.parseLong(finalEvaluationSubjectEntity.getId().toString()));
                        evaluationSubjectQuestionBodyEntity.setEvaluationQuestionBodyId(Long.parseLong(evaluationQuestionBodyEntity.getId().toString()));
                        if (StringUtils.isNotBlank(e.getGroupId())) {
                            evaluationSubjectQuestionBodyEntity.setEvaluationSubjectQuestionGroupId(Long.parseLong(e.getGroupId()));
                        }
                        evaluationSubjectQuestionBodyMapper.insert(evaluationSubjectQuestionBodyEntity);
                    } else {
                        evaluationSubjectQuestionBodyEntity.setEvaluationSubjectId(Long.parseLong(finalEvaluationSubjectEntity.getId().toString()));
                        evaluationSubjectQuestionBodyEntity.setEvaluationQuestionBodyId(Long.parseLong(evaluationQuestionBodyEntity.getId().toString()));
                        if (StringUtils.isNotBlank(e.getGroupId())) {
                            evaluationSubjectQuestionBodyEntity.setEvaluationSubjectQuestionGroupId(Long.parseLong(e.getGroupId()));
                        }
                        evaluationSubjectQuestionBodyMapper.updateById(evaluationSubjectQuestionBodyEntity);
                    }
                }
                //选项
                else {
                    EvaluationQuestionBodySelectorEntity evaluationQuestionBodySelectorEntity = evaluationQuestionBodySelectorEntityMap.get(Long.parseLong(itemid));
                    if (evaluationQuestionBodySelectorEntity == null) {
                        evaluationQuestionBodySelectorEntity = new EvaluationQuestionBodySelectorEntity();
                        evaluationQuestionBodySelectorEntity.setId(Long.parseLong(itemid));
                        evaluationQuestionBodySelectorEntity.setCode(e.getCode());
                        evaluationQuestionBodySelectorEntity.setAnswerContent(e.getName());
                        evaluationQuestionBodySelectorEntity.setDeleteFlag(false);
                        evaluationQuestionBodySelectorEntity.setChooseOnlyFlag(Boolean.FALSE);
                        if (StringUtils.isNotEmpty(e.getChooseOnlyFlag())) {
                            evaluationQuestionBodySelectorEntity.setChooseOnlyFlag(e.getChooseOnlyFlag().equals("1"));
                        }
                        evaluationQuestionBodySelectorEntity.setEvaluationQuestionBodyId(Long.parseLong(e.getParentBodyId()));
                        if (!StringUtils.isEmpty(e.getNextBodyId())) {
                            evaluationQuestionBodySelectorEntity.setChildrenEvaluationQuestionBodyId(Long.parseLong(e.getNextBodyId()));
                        }
                        if (!StringUtils.isEmpty(e.getOrderIndex())) {
                            evaluationQuestionBodySelectorEntity.setSort(Long.parseLong(e.getOrderIndex()));
                        } else {
                            evaluationQuestionBodySelectorEntity.setSort(Long.parseLong(evaluationQuestionBodySelectorEntity.getId().toString()));
                        }
                        evaluationQuestionBodySelectorMapper.insert(evaluationQuestionBodySelectorEntity);
                    } else {
                        evaluationQuestionBodySelectorEntity.setCode(e.getCode());
                        evaluationQuestionBodySelectorEntity.setAnswerContent(e.getName());
                        evaluationQuestionBodySelectorEntity.setChooseOnlyFlag(Boolean.FALSE);
                        if (StringUtils.isNotEmpty(e.getChooseOnlyFlag())) {
                            evaluationQuestionBodySelectorEntity.setChooseOnlyFlag(e.getChooseOnlyFlag().equals("1"));
                        }
                        evaluationQuestionBodySelectorEntity.setEvaluationQuestionBodyId(Long.parseLong(e.getParentBodyId()));
                        if (!StringUtils.isEmpty(e.getNextBodyId())) {
                            evaluationQuestionBodySelectorEntity.setChildrenEvaluationQuestionBodyId(Long.parseLong(e.getNextBodyId()));
                        }
                        if (!StringUtils.isEmpty(e.getOrderIndex())) {
                            evaluationQuestionBodySelectorEntity.setSort(Long.parseLong(e.getOrderIndex()));
                        } else {
                            evaluationQuestionBodySelectorEntity.setSort(Long.parseLong(evaluationQuestionBodySelectorEntity.getId().toString()));
                        }
                        evaluationQuestionBodySelectorMapper.updateById(evaluationQuestionBodySelectorEntity);
                    }
                }
            }
        });

        //模板
        EvaluationTemplateEntity evaluationTemplateEntity = evaluationTemplateMapper.selectById(id);
        if (evaluationTemplateEntity == null) {
            evaluationTemplateEntity = new EvaluationTemplateEntity();
            evaluationTemplateEntity.setTemplateCode(code);
            evaluationTemplateEntity.setName(name);
            evaluationTemplateEntity.setId(Long.parseLong(id));
            evaluationTemplateEntity.setDeleteFlag(false);
            evaluationTemplateMapper.insert(evaluationTemplateEntity);
        } else {
            evaluationTemplateEntity.setName(name);
            evaluationTemplateMapper.updateById(evaluationTemplateEntity);
        }

        if (!code.equalsIgnoreCase(evaluationTemplateEntity.getTemplateCode())) {
            throw new RuntimeException("模板编号不一致");
        }
        //题干
        List<EvaluationTemplateQuestionBodyEntity> evaluationTemplateQuestionBodyList = ListUtils.emptyIfNull(evaluationTemplateQuestionBodyMapper.selectList(Wrappers.lambdaQuery(EvaluationTemplateQuestionBodyEntity.class)
                .eq(EvaluationTemplateQuestionBodyEntity::getEvaluationTemplateId, evaluationTemplateEntity.getId())));
        //转成map
        Map<Long, EvaluationTemplateQuestionBodyEntity> evaluationTemplateQuestionBodyEntityMap = evaluationTemplateQuestionBodyList.stream().collect(Collectors.toMap(EvaluationTemplateQuestionBodyEntity::getEvaluationQuestionBodyId, item -> item));

        //选项
        List<EvaluationTemplateQuestionBodySelectorEntity> evaluationTemplateQuestionBodySelectorList = ListUtils.emptyIfNull(evaluationTemplateQuestionBodySelectorMapper.selectList(Wrappers.lambdaQuery(EvaluationTemplateQuestionBodySelectorEntity.class)
                .eq(EvaluationTemplateQuestionBodySelectorEntity::getEvaluationTemplateId, evaluationTemplateEntity.getId())));
        //转成map
        Map<Long, EvaluationTemplateQuestionBodySelectorEntity> evaluationTemplateQuestionBodySelectorEntityMap = evaluationTemplateQuestionBodySelectorList.stream().collect(Collectors.toMap(e->Long.parseLong(e.getId().toString()), item -> item));

        EvaluationTemplateEntity finalEvaluationTemplateEntity = evaluationTemplateEntity;
        result.getList().stream().forEach(e->{
            String itemid = e.getId();
            if (!(itemid.endsWith("00000000") && !itemid.endsWith("000000000"))) {
                //题干
                if (!StringUtils.isEmpty(e.getType())) {
                    EvaluationTemplateQuestionBodyEntity evaluationTemplateQuestionBodyEntity = evaluationTemplateQuestionBodyEntityMap.get(Long.parseLong(itemid));
                    if (evaluationTemplateQuestionBodyEntity == null) {
                        evaluationTemplateQuestionBodyEntity = new EvaluationTemplateQuestionBodyEntity();
                        evaluationTemplateQuestionBodyEntity.setCode(e.getCode());
                        evaluationTemplateQuestionBodyEntity.setDeleteFlag(false);
                        evaluationTemplateQuestionBodyEntity.setRequireFlag(true);
                        evaluationTemplateQuestionBodyEntity.setId(Long.parseLong(itemid));
                        evaluationTemplateQuestionBodyEntity.setEvaluationQuestionBodyId(evaluationTemplateQuestionBodyEntity.getId());
                        evaluationTemplateQuestionBodyEntity.setEvaluationTemplateId(finalEvaluationTemplateEntity.getId());
                        if (!StringUtils.isEmpty(e.getScore())) {
                            evaluationTemplateQuestionBodyEntity.setScore(new BigDecimal(e.getScore()));
                        }
                        if (!StringUtils.isEmpty(e.getOrderIndex())) {
                            evaluationTemplateQuestionBodyEntity.setSort(Long.parseLong(e.getOrderIndex()));
                        } else {
                            evaluationTemplateQuestionBodyEntity.setSort(Long.parseLong(evaluationTemplateQuestionBodyEntity.getId().toString()));
                        }
                        evaluationTemplateQuestionBodyEntity.setJsonPath(e.getJsonPath());
                        evaluationTemplateQuestionBodyEntity.setConfig(e.getConfig());
                        evaluationTemplateQuestionBodyMapper.insert(evaluationTemplateQuestionBodyEntity);
                    } else {
                        evaluationTemplateQuestionBodyEntity.setCode(e.getCode());
                        evaluationTemplateQuestionBodyEntity.setEvaluationQuestionBodyId(evaluationTemplateQuestionBodyEntity.getId());
                        evaluationTemplateQuestionBodyEntity.setEvaluationTemplateId(finalEvaluationTemplateEntity.getId());
                        evaluationTemplateQuestionBodyEntity.setRequireFlag(true);
                        if (!StringUtils.isEmpty(e.getScore())) {
                            evaluationTemplateQuestionBodyEntity.setScore(new BigDecimal(e.getScore()));
                        }
                        if (!StringUtils.isEmpty(e.getOrderIndex())) {
                            evaluationTemplateQuestionBodyEntity.setSort(Long.parseLong(e.getOrderIndex()));
                        } else {
                            evaluationTemplateQuestionBodyEntity.setSort(Long.parseLong(evaluationTemplateQuestionBodyEntity.getId().toString()));
                        }
                        evaluationTemplateQuestionBodyEntity.setJsonPath(e.getJsonPath());
                        evaluationTemplateQuestionBodyEntity.setConfig(e.getConfig());
                        evaluationTemplateQuestionBodyMapper.updateById(evaluationTemplateQuestionBodyEntity);
                    }
                } else {
                    //选项
                    EvaluationTemplateQuestionBodySelectorEntity evaluationTemplateQuestionBodySelectorEntity = evaluationTemplateQuestionBodySelectorEntityMap.get(Long.parseLong(itemid));
                    if (evaluationTemplateQuestionBodySelectorEntity == null) {
                        evaluationTemplateQuestionBodySelectorEntity = new EvaluationTemplateQuestionBodySelectorEntity();
                        evaluationTemplateQuestionBodySelectorEntity.setDeleteFlag(false);
                        evaluationTemplateQuestionBodySelectorEntity.setId(Long.parseLong(itemid));
                        evaluationTemplateQuestionBodySelectorEntity.setEvaluationTemplateId(finalEvaluationTemplateEntity.getId());
                        evaluationTemplateQuestionBodySelectorEntity.setEvaluationTemplateQuestionBodyId(Long.parseLong(e.getParentBodyId()));
                        evaluationTemplateQuestionBodySelectorEntity.setEvaluationQuestionBodyId(Long.parseLong(e.getParentBodyId()));
                        evaluationTemplateQuestionBodySelectorEntity.setEvaluationQuestionBodySelectorId(Long.parseLong(evaluationTemplateQuestionBodySelectorEntity.getId().toString()));
                        evaluationTemplateQuestionBodySelectorEntity.setChooseOnlyFlag(Boolean.FALSE);
                        if (StringUtils.isNotEmpty(e.getChooseOnlyFlag())) {
                            evaluationTemplateQuestionBodySelectorEntity.setChooseOnlyFlag(e.getChooseOnlyFlag().equals("1"));
                        }
                        if (!StringUtils.isEmpty(e.getScore())) {
                            evaluationTemplateQuestionBodySelectorEntity.setScore(new BigDecimal(e.getScore()));
                        }
                        if (!StringUtils.isEmpty(e.getOrderIndex())) {
                            evaluationTemplateQuestionBodySelectorEntity.setSort(Long.parseLong(e.getOrderIndex()));
                        } else {
                            evaluationTemplateQuestionBodySelectorEntity.setSort(Long.parseLong(evaluationTemplateQuestionBodySelectorEntity.getId().toString()));
                        }
                        if (!StringUtils.isEmpty(e.getOrderIndex())) {
                            evaluationTemplateQuestionBodySelectorEntity.setSort(Long.parseLong(e.getOrderIndex()));
                        } else {
                            evaluationTemplateQuestionBodySelectorEntity.setSort(Long.parseLong(evaluationTemplateQuestionBodySelectorEntity.getId().toString()));
                        }
                        evaluationTemplateQuestionBodySelectorEntity.setConfig(e.getConfig());
                        evaluationTemplateQuestionBodySelectorMapper.insert(evaluationTemplateQuestionBodySelectorEntity);
                    } else {
                        evaluationTemplateQuestionBodySelectorEntity.setEvaluationTemplateId(finalEvaluationTemplateEntity.getId());
                        evaluationTemplateQuestionBodySelectorEntity.setEvaluationTemplateQuestionBodyId(Long.parseLong(e.getParentBodyId()));
                        evaluationTemplateQuestionBodySelectorEntity.setEvaluationQuestionBodyId(Long.parseLong(e.getParentBodyId()));
                        evaluationTemplateQuestionBodySelectorEntity.setEvaluationQuestionBodySelectorId(Long.parseLong(evaluationTemplateQuestionBodySelectorEntity.getId().toString()) );
                        evaluationTemplateQuestionBodySelectorEntity.setChooseOnlyFlag(Boolean.FALSE);
                        if (StringUtils.isNotEmpty(e.getChooseOnlyFlag())) {
                            evaluationTemplateQuestionBodySelectorEntity.setChooseOnlyFlag(e.getChooseOnlyFlag().equals("1"));
                        }
                        if (!StringUtils.isEmpty(e.getScore())) {
                            evaluationTemplateQuestionBodySelectorEntity.setScore(new BigDecimal(e.getScore()));
                        }
                        if (!StringUtils.isEmpty(e.getOrderIndex())) {
                            evaluationTemplateQuestionBodySelectorEntity.setSort(Long.parseLong(e.getOrderIndex()));
                        } else {
                            evaluationTemplateQuestionBodySelectorEntity.setSort(Long.parseLong(evaluationTemplateQuestionBodySelectorEntity.getId().toString()));
                        }
                        evaluationTemplateQuestionBodySelectorEntity.setConfig(e.getConfig());
                        evaluationTemplateQuestionBodySelectorMapper.updateById(evaluationTemplateQuestionBodySelectorEntity);
                    }
                }
            }
        });
    }

}
