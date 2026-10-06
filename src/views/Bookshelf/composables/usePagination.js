import { ref, computed } from 'vue'

// 书架分页：每页 3 行 × 4 本 = 12 本
export function usePagination(filteredBooks) {
  const currentPage = ref(1)
  const itemsPerRow = 4 // 每行4本书
  const rowsPerPage = 3 // 每页3行
  const itemsPerPage = itemsPerRow * rowsPerPage // 每页12本书

  // 分页后的书籍
  const paginatedBooks = computed(() => {
    const start = (currentPage.value - 1) * itemsPerPage
    const end = start + itemsPerPage
    return filteredBooks.value.slice(start, end)
  })

  // 总页数
  const totalPages = computed(() => {
    return Math.ceil(filteredBooks.value.length / itemsPerPage)
  })

  // 切换页码
  const goToPage = (page) => {
    if (page >= 1 && page <= totalPages.value) {
      currentPage.value = page
    }
  }

  // 上一页
  const prevPage = () => {
    if (currentPage.value > 1) {
      currentPage.value--
    }
  }

  // 下一页
  const nextPage = () => {
    if (currentPage.value < totalPages.value) {
      currentPage.value++
    }
  }

  return { currentPage, paginatedBooks, totalPages, goToPage, prevPage, nextPage }
}
