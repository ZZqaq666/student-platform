import { ref } from 'vue'
import { apiService } from '@/api/api'

// 书籍详情弹窗数据域（含推荐书籍/用户评价）
export function useBookDetail({ chapters, updateLastClicked }) {
  // 书籍详情弹窗
  const showBookDetailModal = ref(false)
  const selectedBook = ref(null)
  const showFullCatalog = ref(false)

  // 书籍推荐和用户评价
  const bookRecommendations = ref(null)
  const recommendationsLoading = ref(false)

  // 打开书籍详情
  const openBookDetail = async (book) => {
    selectedBook.value = book
    showBookDetailModal.value = true
    showFullCatalog.value = false
    updateLastClicked(book)

    // 获取书籍推荐和用户评价
    await fetchBookRecommendations(book.id)
  }

  // 切换完整目录显示
  const toggleFullCatalog = async () => {
    showFullCatalog.value = !showFullCatalog.value
    // 如果显示完整目录，获取章节数据
    if (showFullCatalog.value && selectedBook.value) {
      await chapters.fetchChapters(selectedBook.value.id)
    }
  }

  // 获取书籍推荐和用户评价
  const fetchBookRecommendations = async (bookId) => {
    try {
      recommendationsLoading.value = true
      const response = await apiService.bookshelf.getBookRecommendations(bookId)
      // 由于响应拦截器已经返回了response.data，所以直接使用response
      bookRecommendations.value = response

      // 如果后端返回的推荐数据为空，使用mock数据作为回退
      if (!bookRecommendations.value ||
          !bookRecommendations.value.recommendedBooks ||
          bookRecommendations.value.recommendedBooks.length === 0 ||
          !bookRecommendations.value.userReviews ||
          bookRecommendations.value.userReviews.length === 0) {
        console.log('后端返回的推荐数据为空，使用mock数据:', bookRecommendations.value)
        bookRecommendations.value = generateMockRecommendations(bookId)
      }
    } catch (error) {
      console.error('获取书籍推荐失败，使用mock数据:', error)
      // 使用mock数据作为回退
      bookRecommendations.value = generateMockRecommendations(bookId)
    } finally {
      recommendationsLoading.value = false
    }
  }

  // 生成书籍推荐和用户评价的mock数据
  const generateMockRecommendations = (bookId) => {
    // 根据书籍ID生成相应的推荐书籍和用户评价
    const mockData = {
      recommendedBooks: [],
      userReviews: []
    }

    // 高等数学
    if (bookId === 205 || bookId === 206) {
      mockData.recommendedBooks = [
        {
          id: 207,
          title: '线性代数',
          author: '同济大学数学系',
          coverImage: '',
          subject: '数学',
          recommendationReason: '线性代数是高等数学的重要基础，两本书内容相辅相成'
        },
        {
          id: 208,
          title: '概率论与数理统计',
          author: '浙江大学',
          coverImage: '',
          subject: '数学',
          recommendationReason: '概率论与数理统计是高等数学的延伸，适合进一步学习'
        }
      ]
      mockData.userReviews = [
        {
          id: 1,
          username: '数学爱好者',
          rating: 5,
          content: '这本书内容详实，讲解清晰，是学习高等数学的好帮手。',
          createdAt: '2026-03-15',
          likes: 12
        },
        {
          id: 2,
          username: '大学生',
          rating: 4,
          content: '例题丰富，习题有针对性，对考试很有帮助。',
          createdAt: '2026-03-20',
          likes: 8
        }
      ]
    }
    // 大学物理
    else if (bookId === 209 || bookId === 210) {
      mockData.recommendedBooks = [
        {
          id: 205,
          title: '高等数学（上册）',
          author: '同济大学数学系',
          coverImage: '',
          subject: '数学',
          recommendationReason: '物理学需要扎实的数学基础，高等数学是必备的'
        },
        {
          id: 207,
          title: '线性代数',
          author: '同济大学数学系',
          coverImage: '',
          subject: '数学',
          recommendationReason: '线性代数在物理学中有广泛应用，特别是在量子力学中'
        }
      ]
      mockData.userReviews = [
        {
          id: 3,
          username: '物理迷',
          rating: 5,
          content: '这本书深入浅出，物理概念解释得非常清楚，推荐给所有学习物理的同学。',
          createdAt: '2026-03-18',
          likes: 15
        },
        {
          id: 4,
          username: '理工科学生',
          rating: 4,
          content: '实验部分的讲解很详细，有助于理解物理概念。',
          createdAt: '2026-03-22',
          likes: 6
        }
      ]
    }
    // 计算机相关课程
    else if ([211, 212, 213, 214, 216].includes(bookId)) {
      mockData.recommendedBooks = [
        {
          id: 212,
          title: '计算机网络',
          author: '谢希仁',
          coverImage: '',
          subject: '计算机',
          recommendationReason: '计算机网络是计算机专业的核心课程，与本课程内容相关'
        },
        {
          id: 213,
          title: '操作系统原理',
          author: '汤小丹',
          coverImage: '',
          subject: '计算机',
          recommendationReason: '操作系统是计算机系统的基础，与本课程内容密切相关'
        }
      ]
      mockData.userReviews = [
        {
          id: 5,
          username: '程序员',
          rating: 5,
          content: '这本书的内容非常实用，代码示例丰富，对实际开发很有帮助。',
          createdAt: '2026-03-16',
          likes: 20
        },
        {
          id: 6,
          username: '计算机专业学生',
          rating: 4,
          content: '讲解清晰，概念明确，是学习计算机相关课程的好教材。',
          createdAt: '2026-03-21',
          likes: 10
        }
      ]
    }
    // 英语课程
    else if (bookId === 222) {
      mockData.recommendedBooks = [
        {
          id: 223,
          title: '新视野大学英语2（第三版）',
          author: '郑树棠',
          coverImage: '',
          subject: '英语',
          recommendationReason: '是本教材的续册，内容进阶，适合进一步学习'
        },
        {
          id: 224,
          title: '英语听力教程',
          author: '张民伦',
          coverImage: '',
          subject: '英语',
          recommendationReason: '提高英语听力能力，与本教材配套使用效果更佳'
        }
      ]
      mockData.userReviews = [
        {
          id: 7,
          username: '英语学习者',
          rating: 5,
          content: '教材内容新颖，话题丰富，有助于提高英语综合能力。',
          createdAt: '2026-03-17',
          likes: 18
        },
        {
          id: 8,
          username: '大学生',
          rating: 4,
          content: '配套资源丰富，有音频和视频，学习体验很好。',
          createdAt: '2026-03-23',
          likes: 9
        }
      ]
    }
    // 其他课程
    else {
      mockData.recommendedBooks = [
        {
          id: 205,
          title: '高等数学（上册）',
          author: '同济大学数学系',
          coverImage: '',
          subject: '数学',
          recommendationReason: '高等数学是大学学习的基础课程，对各专业都有帮助'
        },
        {
          id: 222,
          title: '新视野大学英语1（第三版）',
          author: '郑树棠',
          coverImage: '',
          subject: '英语',
          recommendationReason: '英语是国际化交流的工具，对未来发展很重要'
        }
      ]
      mockData.userReviews = [
        {
          id: 9,
          username: '学习者',
          rating: 4,
          content: '这本书内容丰富，讲解详细，是学习本课程的好教材。',
          createdAt: '2026-03-19',
          likes: 7
        },
        {
          id: 10,
          username: '大学生',
          rating: 3,
          content: '教材难度适中，适合课堂学习使用。',
          createdAt: '2026-03-24',
          likes: 4
        }
      ]
    }

    return mockData
  }

  return {
    showBookDetailModal, selectedBook, showFullCatalog,
    bookRecommendations, recommendationsLoading,
    openBookDetail, toggleFullCatalog
  }
}
