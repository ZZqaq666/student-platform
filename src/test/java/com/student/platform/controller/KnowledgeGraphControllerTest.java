package com.student.platform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.student.platform.dto.KnowledgeGraphDTO;
import com.student.platform.dto.KnowledgeNodeDTO;
import com.student.platform.dto.KnowledgeRelationDTO;
import com.student.platform.service.KnowledgeGraphService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class KnowledgeGraphControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private KnowledgeGraphService knowledgeGraphService;

    @Test
    public void testGetKnowledgeGraphByBook() throws Exception {
        List<KnowledgeNodeDTO> nodes = new ArrayList<>();
        KnowledgeNodeDTO node = new KnowledgeNodeDTO();
        node.setId(1L);
        node.setBookId(1L);
        node.setParentId(null);
        node.setName("高等数学");
        node.setDescription("高等数学基础课程");
        node.setLevel(1);
        node.setCategory(0);
        node.setValue(100);
        node.setSymbolSize(80);
        node.setCreatedAt(LocalDateTime.now());
        node.setUpdatedAt(LocalDateTime.now());
        nodes.add(node);

        List<KnowledgeRelationDTO> relations = new ArrayList<>();
        KnowledgeRelationDTO relation = new KnowledgeRelationDTO();
        relation.setId(1L);
        relation.setSourceNodeId(1L);
        relation.setSourceNodeName("高等数学");
        relation.setTargetNodeId(2L);
        relation.setTargetNodeName("微积分");
        relation.setRelationType("CONTAINS");
        relation.setCreatedAt(LocalDateTime.now());
        relation.setUpdatedAt(LocalDateTime.now());
        relations.add(relation);

        KnowledgeGraphDTO graphDTO = new KnowledgeGraphDTO(nodes, relations);

        Mockito.when(knowledgeGraphService.getKnowledgeGraphByBook(1L)).thenReturn(graphDTO);

        MvcResult result = mockMvc.perform(get("/knowledge-graph/book/1"))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        assertNotNull(response);
        assertTrue(response.contains("高等数学"));
        assertTrue(response.contains("nodes"));
        assertTrue(response.contains("relations"));
    }

    @Test
    public void testGetKnowledgeGraphByBookWithCategoryField() throws Exception {
        List<KnowledgeNodeDTO> nodes = new ArrayList<>();
        
        KnowledgeNodeDTO rootNode = new KnowledgeNodeDTO();
        rootNode.setId(1L);
        rootNode.setBookId(1L);
        rootNode.setParentId(null);
        rootNode.setName("核心课程");
        rootNode.setCategory(0);
        rootNode.setSymbolSize(80);
        rootNode.setValue(100);
        nodes.add(rootNode);

        KnowledgeNodeDTO childNode = new KnowledgeNodeDTO();
        childNode.setId(2L);
        childNode.setBookId(1L);
        childNode.setParentId(1L);
        childNode.setName("章节模块");
        childNode.setCategory(1);
        childNode.setSymbolSize(60);
        childNode.setValue(70);
        nodes.add(childNode);

        KnowledgeGraphDTO graphDTO = new KnowledgeGraphDTO(nodes, new ArrayList<>());
        Mockito.when(knowledgeGraphService.getKnowledgeGraphByBook(1L)).thenReturn(graphDTO);

        MvcResult result = mockMvc.perform(get("/knowledge-graph/book/1"))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        assertTrue(response.contains("\"category\":0"));
        assertTrue(response.contains("\"symbolSize\":80"));
        assertTrue(response.contains("\"value\":100"));
    }

    @Test
    public void testGetKnowledgeNodeById() throws Exception {
        KnowledgeNodeDTO node = new KnowledgeNodeDTO();
        node.setId(1L);
        node.setBookId(1L);
        node.setName("测试节点");
        node.setCategory(2);
        node.setSymbolSize(45);
        node.setValue(60);

        Mockito.when(knowledgeGraphService.getKnowledgeNodeById(1L)).thenReturn(node);

        MvcResult result = mockMvc.perform(get("/knowledge-graph/node/1"))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        assertNotNull(response);
        assertTrue(response.contains("测试节点"));
    }

    @Test
    public void testSearchKnowledgeNodes() throws Exception {
        List<KnowledgeNodeDTO> nodes = new ArrayList<>();
        KnowledgeNodeDTO node = new KnowledgeNodeDTO();
        node.setId(1L);
        node.setName("微积分");
        node.setCategory(1);
        nodes.add(node);

        Mockito.when(knowledgeGraphService.searchKnowledgeNodes(1L, "微积分")).thenReturn(nodes);

        MvcResult result = mockMvc.perform(get("/knowledge-graph/book/1/search")
                .param("keyword", "微积分"))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        assertNotNull(response);
        assertTrue(response.contains("微积分"));
    }
}
