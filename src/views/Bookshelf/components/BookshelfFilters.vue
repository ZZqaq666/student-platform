<template>
  <div class="bookshelf-filter">
    <div class="filter-section">
      <div class="filter-label">按专业领域筛选</div>
      <div class="select-container">
        <div class="select-dropdown" v-show="showMajorDropdown">
          <div
            v-for="major in majorOptions"
            :key="major.value"
            class="select-option"
            :class="{ active: majorFilter === major.value }"
            @click="selectMajor(major.value)"
          >
            {{ major.label }}
          </div>
        </div>
        <div class="select-display" @click="toggleMajorDropdown">
          {{ getSelectedMajorName() }}
          <span class="select-arrow">▼</span>
        </div>
      </div>
    </div>
    <div class="filter-section">
      <div class="filter-label">按学期筛选</div>
      <div class="select-container">
        <div class="select-dropdown" v-show="showSemesterDropdown">
          <div
            v-for="semester in semesterOptions"
            :key="semester.value"
            class="select-option"
            :class="{ active: semesterFilter === semester.value }"
            @click="selectSemester(semester.value)"
          >
            {{ semester.label }}
          </div>
        </div>
        <div class="select-display" @click="toggleSemesterDropdown">
          {{ getSelectedSemesterName() }}
          <span class="select-arrow">▼</span>
        </div>
      </div>
    </div>
    <div class="filter-section">
      <div class="filter-label">按课程类型筛选</div>
      <div class="select-container">
        <div class="select-dropdown" v-show="showCourseTypeDropdown">
          <div
            v-for="courseType in courseTypeOptions"
            :key="courseType.value"
            class="select-option"
            :class="{ active: courseTypeFilter === courseType.value }"
            @click="selectCourseType(courseType.value)"
          >
            {{ courseType.label }}
          </div>
        </div>
        <div class="select-display" @click="toggleCourseTypeDropdown">
          {{ getSelectedCourseTypeName() }}
          <span class="select-arrow">▼</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { inject } from 'vue'

const {
  majorFilter, showMajorDropdown, majorOptions, selectMajor, toggleMajorDropdown, getSelectedMajorName,
  semesterFilter, showSemesterDropdown, semesterOptions, selectSemester, toggleSemesterDropdown, getSelectedSemesterName,
  courseTypeFilter, showCourseTypeDropdown, courseTypeOptions, selectCourseType, toggleCourseTypeDropdown, getSelectedCourseTypeName
} = inject('bookshelfCtx')
</script>
