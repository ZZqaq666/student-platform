package com.student.platform.learning.service.impl;

import com.student.platform.learning.entity.UserProfile;
import com.student.platform.learning.entity.UserSubjectPreference;
import com.student.platform.learning.mapper.UserProfileMapper;
import com.student.platform.learning.mapper.UserSubjectPreferenceMapper;
import com.student.platform.learning.service.RecommendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendServiceImpl implements RecommendService {

    private final UserProfileMapper userProfileMapper;
    private final UserSubjectPreferenceMapper userSubjectPreferenceMapper;

}
