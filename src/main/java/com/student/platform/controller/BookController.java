package com.student.platform.controller;

import com.student.platform.dto.BookChapterTreeDTO;
import com.student.platform.dto.BookDTO;
import com.student.platform.dto.BookRecommendationDTO;
import com.student.platform.dto.Result;
import com.student.platform.dto.UserBookDTO;
import com.student.platform.entity.Book;
import com.student.platform.entity.BookChapter;
import com.student.platform.mapper.BookMapper;
import com.student.platform.service.AiService;
import com.student.platform.service.BookKnowledgePointService;
import com.student.platform.service.BookshelfService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.ArrayList;

/**
 * 书籍管理控制器
 * <p>
 * 提供书籍查询、搜索、最近阅读等书籍相关接口
 * </p>
 *
 * @author student-platform
 * @since 1.0.0
 */
@Tag(name = "书籍管理", description = "书籍查询、搜索、最近阅读等接口")
@RestController
@RequestMapping("/books")
@Slf4j
public class BookController {

    private final BookshelfService bookshelfService;
    private final AiService aiService;
    private final BookMapper bookMapper;
    private final BookKnowledgePointService bookKnowledgePointService;
    
    public BookController(BookshelfService bookshelfService, AiService aiService, BookMapper bookMapper, BookKnowledgePointService bookKnowledgePointService) {
        this.bookshelfService = bookshelfService;
        this.aiService = aiService;
        this.bookMapper = bookMapper;
        this.bookKnowledgePointService = bookKnowledgePointService;
        log.info("BookController initialized with bookKnowledgePointService: {}", bookKnowledgePointService != null ? "OK" : "NULL");
    }

    /**
     * 获取所有书籍列表
     *
     * @return 书籍列表
     */
    @Operation(
            summary = "获取所有书籍",
            description = "获取系统中所有可用的书籍列表"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功",
                    content = @Content(schema = @Schema(implementation = Result.class)))
    })
    @GetMapping
    public Result<List<BookDTO>> getAllBooks() {
        log.debug("获取所有书籍列表");
        List<BookDTO> books = bookshelfService.getAllBooks();
        return Result.success(books);
    }

    /**
     * 获取最近学习的书籍
     *
     * @return 最近学习的书籍
     */
    @Operation(
            summary = "获取最近学习的书籍",
            description = "获取当前用户最近学习的书籍，如果用户未登录或未学习任何书籍则返回空"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "401", description = "未登录")
    })
    @GetMapping("/recent")
    public Result<UserBookDTO> getRecentBook() {
        log.debug("获取最近学习的书籍");
        UserBookDTO recentBook = bookshelfService.getRecentBook();
        return Result.success(recentBook);
    }

    /**
     * 根据ID获取书籍详情
     *
     * @param id 书籍ID
     * @return 书籍详情
     */
    @Operation(
            summary = "获取书籍详情",
            description = "根据书籍ID获取详细信息"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "404", description = "书籍不存在")
    })
    @GetMapping("/{id}")
    public Result<BookDTO> getBookById(
            @Parameter(description = "书籍ID", required = true) @PathVariable Long id
    ) {
        log.debug("获取书籍详情, id: {}", id);
        BookDTO book = bookshelfService.getBookById(id);
        return Result.success(book);
    }

    /**
     * 搜索大学教材
     *
     * @param keyword    关键词(标题、作者、专业、学科)
     * @param major      专业
     * @param semester   学期
     * @param courseType 课程类型
     * @return 符合条件的书籍列表
     */
    @Operation(
            summary = "搜索大学教材",
            description = "根据关键词、专业、学期、课程类型搜索大学教材"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "搜索成功")
    })
    @GetMapping("/search")
    public Result<List<BookDTO>> searchBooks(
            @Parameter(description = "关键词，可搜索标题、作者、专业、学科") @RequestParam(required = false) String keyword,
            @Parameter(description = "分类") @RequestParam(required = false) String category,
            @Parameter(description = "专业") @RequestParam(required = false) String major,
            @Parameter(description = "学期") @RequestParam(required = false) String semester,
            @Parameter(description = "课程类型") @RequestParam(required = false) String courseType
    ) {
        log.debug("搜索书籍, keyword: {}, category: {}, major: {}, semester: {}, courseType: {}",
                keyword, category, major, semester, courseType);
        List<BookDTO> books = bookshelfService.searchBooks(keyword, null, major, semester, courseType);
        return Result.success(books);
    }

    /**
     * 根据学科获取书籍
     *
     * @param subject 学科名称
     * @return 书籍列表
     */
    @Operation(
            summary = "根据学科获取书籍",
            description = "获取指定学科的所有书籍"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "400", description = "学科名称不能为空")
    })
    @GetMapping("/subject/{subject}")
    public Result<List<BookDTO>> getBooksBySubject(
            @Parameter(description = "学科名称", required = true) @PathVariable String subject
    ) {
        log.debug("根据学科获取书籍, subject: {}", subject);
        List<BookDTO> books = bookshelfService.getBooksBySubject(subject);
        return Result.success(books);
    }

    /**
     * 获取书籍的章节列表
     *
     * @param id 书籍ID
     * @return 章节列表
     */
    @Operation(
            summary = "获取书籍章节",
            description = "根据书籍ID获取章节列表"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "400", description = "书籍ID无效"),
            @ApiResponse(responseCode = "404", description = "书籍不存在")
    })
    @GetMapping("/{id}/chapters")
    public Result<List<BookChapter>> getBookChapters(
            @Parameter(description = "书籍ID", required = true) @PathVariable Long id
    ) {
        log.debug("获取书籍章节, id: {}", id);
        List<BookChapter> chapters = bookshelfService.getBookChapters(id);
        return Result.success(chapters);
    }

    /**
     * 获取书籍的章节树结构
     *
     * @param id 书籍ID
     * @return 章节树结构
     */
    @Operation(
            summary = "获取书籍章节树",
            description = "根据书籍ID获取章节树结构，支持多级嵌套"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "400", description = "书籍ID无效"),
            @ApiResponse(responseCode = "404", description = "书籍不存在")
    })
    @GetMapping("/{id}/chapters/tree")
    public Result<List<BookChapterTreeDTO>> getBookChapterTree(
            @Parameter(description = "书籍ID", required = true) @PathVariable Long id
    ) {
        log.debug("获取书籍章节树结构, id: {}", id);
        List<BookChapterTreeDTO> chapterTree = bookshelfService.getBookChapterTree(id);
        return Result.success(chapterTree);
    }

    /**
     * 搜索章节
     *
     * @param id      书籍ID
     * @param keyword 搜索关键词
     * @return 符合条件的章节列表
     */
    @Operation(
            summary = "搜索章节",
            description = "根据书籍ID和关键词搜索章节"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "搜索成功"),
            @ApiResponse(responseCode = "400", description = "书籍ID无效"),
            @ApiResponse(responseCode = "404", description = "书籍不存在")
    })
    @GetMapping("/{id}/chapters/search")
    public Result<List<BookChapter>> searchChapters(
            @Parameter(description = "书籍ID", required = true) @PathVariable Long id,
            @Parameter(description = "搜索关键词", required = true) @RequestParam String keyword
    ) {
        log.debug("搜索章节, id: {}, keyword: {}", id, keyword);
        List<BookChapter> chapters = bookshelfService.searchChapters(id, keyword);
        return Result.success(chapters);
    }

    /**
     * 获取书籍推荐和用户评价
     *
     * @param id 书籍ID
     * @return 书籍推荐和用户评价
     */
    @Operation(
            summary = "获取书籍推荐和用户评价",
            description = "根据书籍ID获取相似书籍推荐和用户评价数据"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "400", description = "书籍ID无效"),
            @ApiResponse(responseCode = "404", description = "书籍不存在")
    })
    @GetMapping("/{id}/recommendations")
    public Result<BookRecommendationDTO> getBookRecommendations(
            @Parameter(description = "书籍ID", required = true) @PathVariable Long id
    ) {
        log.debug("获取书籍推荐和用户评价, id: {}", id);
        BookRecommendationDTO recommendations = aiService.getBookRecommendations(id);
        return Result.success(recommendations);
    }

    /**
     * 获取书籍高频知识点
     *
     * @param id 书籍ID
     * @return 高频知识点列表
     */
    @Operation(
            summary = "获取书籍高频知识点",
            description = "根据书籍ID获取高频知识点列表"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "400", description = "书籍ID无效"),
            @ApiResponse(responseCode = "404", description = "书籍不存在")
    })
    @GetMapping("/{id}/high-frequency")
    public Result<List<String>> getHighFrequencyKnowledge(
            @Parameter(description = "书籍ID", required = true) @PathVariable Long id
    ) {
        log.debug("获取书籍高频知识点, id: {}", id);
        try {
            // 检查书籍是否存在
            Book book = bookMapper.selectById(id);
            if (book == null) {
                return Result.success(new ArrayList<>());
            }
            
            // 从数据库获取高频知识点
            List<String> highFrequencyKnowledge = bookKnowledgePointService.getHighFrequencyKnowledgePoints(id);
            
            // 直接返回从数据库获取的知识点，不使用默认值
            return Result.success(highFrequencyKnowledge);
        } catch (Exception e) {
            log.error("获取书籍高频知识点异常: {}", e.getMessage(), e);
            return Result.success(new ArrayList<>());
        }
    }

    /**
     * 获取书籍相关知识点
     *
     * @param id 书籍ID
     * @return 相关知识点列表
     */
    @Operation(
            summary = "获取书籍相关知识点",
            description = "根据书籍ID获取相关知识点列表"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "400", description = "书籍ID无效"),
            @ApiResponse(responseCode = "404", description = "书籍不存在")
    })
    @GetMapping("/{id}/related-nodes")
    public Result<List<String>> getRelatedNodes(
            @Parameter(description = "书籍ID", required = true) @PathVariable Long id
    ) {
        log.debug("获取书籍相关知识点, id: {}", id);
        try {
            // 检查书籍是否存在
            Book book = bookMapper.selectById(id);
            if (book == null) {
                return Result.success(new ArrayList<>());
            }
            
            // 从数据库获取相关知识点
            List<String> relatedNodes = bookKnowledgePointService.getRelatedKnowledgePoints(id);
            
            // 直接返回从数据库获取的知识点，不使用默认值
            return Result.success(relatedNodes);
        } catch (Exception e) {
            log.error("获取书籍相关知识点异常: {}", e.getMessage(), e);
            return Result.success(new ArrayList<>());
        }
    }
    
    /**
     * 测试书籍知识点
     *
     * @param id 书籍ID
     * @return 知识点测试结果
     */
    @Operation(
            summary = "测试书籍知识点",
            description = "测试书籍知识点获取"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "测试成功")
    })
    @GetMapping("/{id}/test-knowledge-points")
    public Result<java.util.Map<String, Object>> testKnowledgePoints(
            @Parameter(description = "书籍ID", required = true) @PathVariable Long id
    ) {
        log.debug("测试书籍知识点, id: {}", id);
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        try {
            // 从数据库获取相关知识点
            List<String> relatedPoints = bookKnowledgePointService.getRelatedKnowledgePoints(id);
            result.put("relatedPoints", relatedPoints);
            
            // 从数据库获取高频知识点
            List<String> highFrequencyPoints = bookKnowledgePointService.getHighFrequencyKnowledgePoints(id);
            result.put("highFrequencyPoints", highFrequencyPoints);
            
            // 检查是否为空
            result.put("relatedPointsEmpty", relatedPoints.isEmpty());
            result.put("highFrequencyPointsEmpty", highFrequencyPoints.isEmpty());
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("测试书籍知识点异常: {}", e.getMessage(), e);
            result.put("error", e.getMessage());
            return Result.success(result);
        }
    }
    
    /**
     * 批量添加教材
     *
     * @param request 包含书籍列表的请求体
     * @return 添加结果
     */
    @Operation(
            summary = "批量添加教材",
            description = "批量添加教材到用户书架"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "添加成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "401", description = "未登录")
    })
    @PostMapping("/add")
    public Result<java.util.Map<String, Object>> addBooks(
            @Parameter(description = "用户ID", required = true) @RequestParam Long userId,
            @Parameter(description = "书籍列表", required = true) @RequestBody java.util.Map<String, java.util.List<BookDTO>> request
    ) {
        log.debug("批量添加教材, userId: {}, request: {}", userId, request);
        try {
            java.util.Map<String, Object> result = bookshelfService.addBooks(userId, request);
            return Result.success(result);
        } catch (Exception e) {
            log.error("批量添加教材失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }
}
