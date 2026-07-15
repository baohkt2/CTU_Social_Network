package com.ctuconnect.service;

import com.ctuconnect.dto.CategoryDTO;
import com.ctuconnect.entity.*;
import com.ctuconnect.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.util.List;

import java.util.stream.Collectors;

@Service
public class CategoryService {

    @Autowired
    private CollegeRepository collegeRepository;

    @Autowired
    private FacultyRepository facultyRepository;

    @Autowired
    private MajorRepository majorRepository;

    @Autowired
    private BatchRepository batchRepository;

    @Autowired
    private GenderRepository genderRepository;

    @Autowired
    private PositionRepository positionRepository;

    @Autowired
    private AcademicRepository academicRepository;

    @Autowired
    private DegreeRepository degreeRepository;

    // ===================== HIERARCHICAL DATA RETRIEVAL =====================

    public CategoryDTO.HierarchicalCategories getAllCategoriesHierarchical() {
        try {
            // Use custom Cypher queries to avoid circular reference issues with Spring Data Neo4j
            // The entities have bidirectional relationships (College -> Faculty -> College) that cause MappingException
            // when using findAll() which triggers eager-fetching of all relationships.
            
            // Query colleges as flat nodes
            List<CollegeEntity> colleges = collegeRepository.findAllFlat();
            
            // Build hierarchical structure by querying faculties/majors separately
            List<CategoryDTO.CollegeInfo> collegeInfos = colleges.stream()
                    .map(college -> {
                        List<FacultyEntity> faculties = facultyRepository.findFlatByCollegeName(college.getName());
    
                        List<CategoryDTO.FacultyInfo> facultyInfos = faculties.stream()
                                .map(faculty -> {
                                    List<MajorEntity> majors = majorRepository.findFlatByFacultyName(faculty.getName());
    
                                    List<CategoryDTO.MajorInfo> majorInfos = majors.stream()
                                            .map(major -> CategoryDTO.MajorInfo.builder()
                                                    .name(major.getName())
                                                    .code(major.getCode())
                                                    .build())
                                            .collect(Collectors.toList());
    
                                    return CategoryDTO.FacultyInfo.builder()
                                            .name(faculty.getName())
                                            .code(faculty.getCode())
                                            .majors(majorInfos)
                                            .build();
                                })
                                .collect(Collectors.toList());
    
                        return CategoryDTO.CollegeInfo.builder()
                                .name(college.getName())
                                .code(college.getCode())
                                .faculties(facultyInfos)
                                .build();
                    })
                    .collect(Collectors.toList());
    
            List<CategoryDTO.BatchInfo> batchInfos = batchRepository.findAllFlat().stream()
                    .map(this::convertToBatchInfo)
                    .collect(Collectors.toList());
    
            List<CategoryDTO.GenderInfo> genderInfos = genderRepository.findAllFlat().stream()
                    .map(this::convertToGenderInfo)
                    .collect(Collectors.toList());
    
            // Get positions, academic titles, and degrees
            List<CategoryDTO.PositionInfo> positionInfos = getAllPositions();
            List<CategoryDTO.AcademicInfo> academicInfos = getAllAcademic();
            List<CategoryDTO.DegreeInfo> degreeInfos = getAllDegrees();
    
            return CategoryDTO.HierarchicalCategories.builder()
                    .colleges(collegeInfos)
                    .batches(batchInfos)
                    .genders(genderInfos)
                    .positions(positionInfos)
                    .academics(academicInfos)
                    .degrees(degreeInfos)
                    .build();
        } catch (Exception e) {
            System.err.println("CRITICAL ERROR IN getAllCategoriesHierarchical:");
            e.printStackTrace();
            throw e;
        }
    }

    private List<CategoryDTO.AcademicInfo> getAllAcademic() {
        List<AcademicEntity> academics = academicRepository.findAll();
        return academics.stream()
                .map(academic -> CategoryDTO.AcademicInfo.builder()
                        .name(academic.getName())
                        .code(academic.getCode())
                        .build())
                .collect(Collectors.toList());
    }

    private List<CategoryDTO.DegreeInfo> getAllDegrees() {
        List<DegreeEntity> degrees = degreeRepository.findAll();
        return degrees.stream()
                .map(degree -> CategoryDTO.DegreeInfo.builder()
                        .name(degree.getName())
                        .code(degree.getCode())
                        .build())
                .collect(Collectors.toList());
    }

    private List<CategoryDTO.PositionInfo> getAllPositions() {

        List<PositionEntity> positions = positionRepository.findAll();
        return positions.stream()
                .map(position -> CategoryDTO.PositionInfo.builder()
                        .name(position.getName())
                        .code(position.getCode())
                        .build())
                .collect(Collectors.toList());
    }


    // ===================== COLLEGE OPERATIONS =====================

    public List<CategoryDTO.CollegeInfo> getAllColleges() {
        List<CollegeEntity> colleges = collegeRepository.findAll();
        return colleges.stream()
                .map(college -> {
                    // Get faculties for this college
                    List<FacultyEntity> faculties = facultyRepository.findByCollegeName(college.getName());

                    List<CategoryDTO.FacultyInfo> facultyInfos = faculties.stream()
                            .map(faculty -> {
                                // Get majors for this faculty
                                List<MajorEntity> majors = majorRepository.findByFacultyName(faculty.getName());

                                List<CategoryDTO.MajorInfo> majorInfos = majors.stream()
                                        .map(major -> CategoryDTO.MajorInfo.builder()
                                                .name(major.getName())
                                                .code(major.getCode())
                                                .build())
                                        .collect(Collectors.toList());

                                return CategoryDTO.FacultyInfo.builder()
                                        .name(faculty.getName())
                                        .code(faculty.getCode())
                                        .majors(majorInfos)
                                        .build();
                            })
                            .collect(Collectors.toList());

                    return CategoryDTO.CollegeInfo.builder()
                            .name(college.getName())
                            .code(college.getCode())
                            .faculties(facultyInfos)
                            .build();
                })
                .collect(Collectors.toList());
    }

    public CategoryDTO.CollegeInfo getCollegeByName(String name) {
        CollegeEntity college = collegeRepository.findByName(name)
                .orElseThrow(() -> new RuntimeException("College not found: " + name));

        // Get faculties for this college
        List<FacultyEntity> faculties = facultyRepository.findByCollegeName(college.getName());

        List<CategoryDTO.FacultyInfo> facultyInfos = faculties.stream()
                .map(faculty -> {
                    // Get majors for this faculty
                    List<MajorEntity> majors = majorRepository.findByFacultyName(faculty.getName());

                    List<CategoryDTO.MajorInfo> majorInfos = majors.stream()
                            .map(major -> CategoryDTO.MajorInfo.builder()
                                    .name(major.getName())
                                    .code(major.getCode())
                                    .build())
                            .collect(Collectors.toList());

                    return CategoryDTO.FacultyInfo.builder()
                            .name(faculty.getName())
                            .code(faculty.getCode())
                            .majors(majorInfos)
                            .build();
                })
                .collect(Collectors.toList());

        return CategoryDTO.CollegeInfo.builder()
                .name(college.getName())
                .code(college.getCode())
                .faculties(facultyInfos)
                .build();
    }

    // ===================== FACULTY OPERATIONS =====================

    public List<CategoryDTO.FacultyInfo> getAllFaculties() {
        List<FacultyEntity> faculties = facultyRepository.findAll();
        return faculties.stream()
                .map(faculty -> {
                    // Get majors for this faculty
                    List<MajorEntity> majors = majorRepository.findByFacultyName(faculty.getName());

                    List<CategoryDTO.MajorInfo> majorInfos = majors.stream()
                            .map(major -> CategoryDTO.MajorInfo.builder()
                                    .name(major.getName())
                                    .code(major.getCode())
                                    .build())
                            .collect(Collectors.toList());

                    CategoryDTO.CollegeBasicInfo college = CategoryDTO.CollegeBasicInfo.builder()
                            .name(faculty.getCollege() != null ? faculty.getCollege().getName() : "")
                            .build();

                    return CategoryDTO.FacultyInfo.builder()
                            .name(faculty.getName())
                            .code(faculty.getCode())
                            .college(college)
                            .majors(majorInfos)
                            .build();
                })
                .collect(Collectors.toList());
    }

    public List<CategoryDTO.FacultyInfo> getFacultiesByCollege(String collegeName) {
        List<FacultyEntity> faculties = facultyRepository.findByCollegeName(collegeName);
        return faculties.stream()
                .map(faculty -> {
                    // Get majors for this faculty
                    List<MajorEntity> majors = majorRepository.findByFacultyName(faculty.getName());

                    List<CategoryDTO.MajorInfo> majorInfos = majors.stream()
                            .map(major -> CategoryDTO.MajorInfo.builder()
                                    .name(major.getName())
                                    .code(major.getCode())
                                    .build())
                            .collect(Collectors.toList());

                    CategoryDTO.CollegeBasicInfo college = CategoryDTO.CollegeBasicInfo.builder()
                            .name(faculty.getCollege() != null ? faculty.getCollege().getName() : "")
                            .build();

                    return CategoryDTO.FacultyInfo.builder()
                            .name(faculty.getName())
                            .code(faculty.getCode())
                            .college(college)
                            .majors(majorInfos)
                            .build();
                })
                .collect(Collectors.toList());
    }

    public CategoryDTO.FacultyInfo getFacultyByName(String name) {
        FacultyEntity faculty = facultyRepository.findByName(name)
                .orElseThrow(() -> new RuntimeException("Faculty not found: " + name));

        // Get majors for this faculty
        List<MajorEntity> majors = majorRepository.findByFacultyName(faculty.getName());

        List<CategoryDTO.MajorInfo> majorInfos = majors.stream()
                .map(major -> CategoryDTO.MajorInfo.builder()
                        .name(major.getName())
                        .code(major.getCode())
                        .build())
                .collect(Collectors.toList());

        CategoryDTO.CollegeBasicInfo college = CategoryDTO.CollegeBasicInfo.builder()
                .name(faculty.getCollege() != null ? faculty.getCollege().getName() : "")
                .build();

        return CategoryDTO.FacultyInfo.builder()
                .name(faculty.getName())
                .code(faculty.getCode())
                .college(college)
                .majors(majorInfos)
                .build();
    }

    // ===================== MAJOR OPERATIONS =====================

    public List<CategoryDTO.MajorInfo> getAllMajors() {
        List<MajorEntity> majors = majorRepository.findAll();
        return majors.stream()
                .map(major -> {
                    CategoryDTO.FacultyBasicInfo faculty = CategoryDTO.FacultyBasicInfo.builder()
                            .name(major.getFaculty() != null ? major.getFaculty().getName() : "")
                            .build();

                    return CategoryDTO.MajorInfo.builder()
                            .name(major.getName())
                            .code(major.getCode())
                            .faculty(faculty)
                            .build();
                })
                .collect(Collectors.toList());
    }

    public List<CategoryDTO.MajorInfo> getMajorsByFaculty(String facultyName) {
        List<MajorEntity> majors = majorRepository.findByFacultyName(facultyName);
        return majors.stream()
                .map(major -> {
                    CategoryDTO.FacultyBasicInfo faculty = CategoryDTO.FacultyBasicInfo.builder()
                            .name(major.getFaculty() != null ? major.getFaculty().getName() : "")
                            .build();

                    return CategoryDTO.MajorInfo.builder()
                            .name(major.getName())
                            .code(major.getCode())
                            .faculty(faculty)
                            .build();
                })
                .collect(Collectors.toList());
    }

    public CategoryDTO.MajorInfo getMajorByName(String name) {
        MajorEntity major = majorRepository.findByName(name)
                .orElseThrow(() -> new RuntimeException("Major not found: " + name));

        CategoryDTO.FacultyBasicInfo faculty = CategoryDTO.FacultyBasicInfo.builder()
                .name(major.getFaculty() != null ? major.getFaculty().getName() : "")
                .build();

        return CategoryDTO.MajorInfo.builder()
                .name(major.getName())
                .code(major.getCode())
                .faculty(faculty)
                .build();
    }

    // ===================== BATCH OPERATIONS =====================

    public List<CategoryDTO.BatchInfo> getAllBatches() {
        List<BatchEntity> batches = batchRepository.findAll();
        return batches.stream()
                .map(this::convertToBatchInfo)
                .collect(Collectors.toList());
    }

    public CategoryDTO.BatchInfo getBatchByYear(String year) {
        BatchEntity batch = batchRepository.findByYear(Integer.valueOf(year))
                .orElseThrow(() -> new RuntimeException("Batch not found: " + year));
        return convertToBatchInfo(batch);
    }

    // ===================== GENDER OPERATIONS =====================

    public List<CategoryDTO.GenderInfo> getAllGenders() {
        List<GenderEntity> genders = genderRepository.findAll();
        return genders.stream()
                .map(this::convertToGenderInfo)
                .collect(Collectors.toList());
    }

    public CategoryDTO.GenderInfo getGenderByCode(String code) {
        GenderEntity gender = genderRepository.findByName(code)
                .orElseThrow(() -> new RuntimeException("Gender not found: " + code));
        return convertToGenderInfo(gender);
    }

    // ===================== CONVERSION METHODS =====================

    private CategoryDTO.BatchInfo convertToBatchInfo(BatchEntity batch) {
        return CategoryDTO.BatchInfo.builder()
                .year(String.valueOf(batch.getYear()))
                .build();
    }

    private CategoryDTO.GenderInfo convertToGenderInfo(GenderEntity gender) {
        return CategoryDTO.GenderInfo.builder()
                .code(gender.getName())
                .name(gender.getName())
                .build();
    }
}
