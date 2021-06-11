package dao.service;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.*;
import cn.edu.sustech.cs307.dto.grade.Grade;
import cn.edu.sustech.cs307.dto.grade.HundredMarkGrade;
import cn.edu.sustech.cs307.dto.grade.PassOrFailGrade;
import cn.edu.sustech.cs307.dto.prerequisite.CoursePrerequisite;
import cn.edu.sustech.cs307.dto.prerequisite.Prerequisite;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.*;
import dao.factory.ServiceFactoryIns;


import javax.annotation.Nullable;
import java.sql.*;
import java.sql.Date;
import java.time.DayOfWeek;
import java.util.*;

public class StudentServiceIns implements StudentService {

    /**
     * The priority of EnrollResult should be (if not SUCCESS):
     *
     * COURSE_NOT_FOUND (在coursesection表里面查找sectionId是否存在)
     * > ALREADY_ENROLLED(在std_section检查sectionId是否存在，存在就说明已经加入了)
     * > ALREADY_PASSED(在前一条指令已经满足的前提下判断这门课是否有成绩，有并且通过说明已经通过了)TODO:
     * > PREREQUISITES_NOT_FULFILLED(先修课不满足条件)
     * > COURSE_CONFLICT_FOUND（课程冲突判断，需要知道时间等条件）
     * > COURSE_IS_FULL（课容量已满判断）
     * > UNKNOWN_ERROR（未知错误）
     */

    //需要写一个函数判断一下这门课是否有成绩，以及是否通过？

    /*  *//**
     * Enrolled successfully
     *//*
    SUCCESS,
    *//**
     * Cannot found the course section
     *//*
    COURSE_NOT_FOUND,
    *//**
     * The course section is full
     *//*
    COURSE_IS_FULL,
    *//**
     * The course section is already enrolled by the student
     *//*
    ALREADY_ENROLLED, @看std_section里面是不是有这门课
    *//**
     * The course (of the section) is already passed by the student
     *//*
    ALREADY_PASSED, @看一下这门课程的成绩
    *//**
     * The student misses prerequisites for the course
     *//*

    PREREQUISITES_NOT_FULFILLED, @
    *//**
     * The student's enrolled courses has time conflicts with the section,
     * or has course conflicts (same course) with the section.
     *//*
    COURSE_CONFLICT_FOUND,
    */

    /**
     * Other (unknown) errors
     *//*
    UNKNOWN_ERROR*/
    @Override
    public void addStudent(int userId, int majorId, String firstName, String lastName, Date enrolledDate) {
        String sql = "insert into student values (?, ?, ?, ?);";
        PreparedStatement preparedStatement;
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();

            // add to course table not handling pre
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, userId);
            preparedStatement.setString(2, firstName + " " + lastName);
            preparedStatement.setInt(3, majorId);
            preparedStatement.setDate(4, enrolledDate);
            preparedStatement.execute();

            // close connection
            connection.close();
            preparedStatement.close();
        } catch (SQLException e) {

            throw new IntegrityViolationException();
        }
    }

    @Override
    public List<CourseSearchEntry> searchCourse(int studentId, int semesterId, @Nullable String searchCid, @Nullable String searchName, @Nullable String searchInstructor, @Nullable DayOfWeek searchDayOfWeek, @Nullable Short searchClassTime, @Nullable List<String> searchClassLocations, CourseType searchCourseType, boolean ignoreFull, boolean ignoreConflict, boolean ignorePassed, boolean ignoreMissingPrerequisites, int pageSize, int pageIndex) {
        String selAll = "select *\n" +
                "from (\n" +
                "         select s.section_id,\n" +
                "                semester_id,\n" +
                "                s.course_id,\n" +
                "                s.name                           as section,\n" +
                "                total_capacity,\n" +
                "                left_capacity,\n" +
                "                id_code,\n" +
                "                c.name                           as course,\n" +
                "                credit,\n" +
                "                class_hour,\n" +
                "                grading,\n" +
                "                (c.name || '[' || s.name || ']') as search_name\n" +
                "         from section_semester ss\n" +
                "                  join section s on s.section_id = ss.section_id\n" +
                "                  join course c on c.course_id = s.course_id\n" +
                "         where ss.semester_id = ?\n" +
                "     ) a\n" +
                "order by (a.course_id, search_name);";

        String selAllSection = "select *\n" +
                "from (\n" +
                "         select s.section_id,\n" +
                "                s.course_id,\n" +
                "                s.name                           as section,\n" +
                "                total_capacity,\n" +
                "                left_capacity,\n" +
                "                id_code,\n" +
                "                c.name                           as course,\n" +
                "                credit,\n" +
                "                class_hour,\n" +
                "                grading,\n" +
                "                (c.name || '[' || s.name || ']') as search_name\n" +
                "         from section s\n" +
                "                  join course c on c.course_id = s.course_id\n" +
                "     ) a\n" +
                "order by search_name;";

        String selSelected = "select (c.name || '[' || s2.name || ']') as search_name\n" +
                "from section_semester ss\n" +
                "         join std_section s on ss.section_id = s.section_id\n" +
                "         join section s2 on s2.section_id = s.section_id\n" +
                "         join course c on c.course_id = s2.course_id\n" +
                "where semester_id = ?\n" +
                "and std_id = 1;";

        List<CourseSearchEntry> allCourses = new ArrayList<>();
        List<CourseSearchEntry> totalEntries = new ArrayList<>();
        PreparedStatement preparedStatement;
        ResultSet resultSet;
        CourseService courseService = new CourseServiceIns();
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();

            // execute sel
            preparedStatement = connection.prepareStatement(selAll);
            preparedStatement.setInt(1, semesterId);
            resultSet = preparedStatement.executeQuery();
            CourseSection section;
            while (resultSet.next()) {
                CourseSearchEntry entry = new CourseSearchEntry();

                Course course = new Course();
                course.id = resultSet.getString("id_code");
                course.name = resultSet.getString("course");
                course.grading = Course.CourseGrading.values()[resultSet.getBoolean("grading") ? 1 : 0];
                course.credit = resultSet.getInt("credit");
                course.classHour = resultSet.getInt("class_hour");
                entry.course = course;

                section = new CourseSection();
                section.id = resultSet.getInt("section_id");
                section.name = resultSet.getString("section");
                section.totalCapacity = resultSet.getInt("total_capacity");
                section.leftCapacity = resultSet.getInt("left_capacity");
                entry.section = section;

                entry.sectionClasses = new HashSet<>();
                entry.sectionClasses.addAll(courseService.getCourseSectionClasses(section.id));

                totalEntries.add(entry);
            }

            // execute sel
            preparedStatement = connection.prepareStatement(selAllSection);
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                CourseSearchEntry entry = new CourseSearchEntry();

                Course course = new Course();
                course.id = resultSet.getString("id_code");
                course.name = resultSet.getString("course");
                course.grading = Course.CourseGrading.values()[resultSet.getBoolean("grading") ? 1 : 0];
                course.credit = resultSet.getInt("credit");
                course.classHour = resultSet.getInt("class_hour");
                entry.course = course;

                section = new CourseSection();
                section.id = resultSet.getInt("section_id");
                section.name = resultSet.getString("section");
                section.totalCapacity = resultSet.getInt("total_capacity");
                section.leftCapacity = resultSet.getInt("left_capacity");
                entry.section = section;

                entry.sectionClasses = new HashSet<>();
                entry.sectionClasses.addAll(courseService.getCourseSectionClasses(section.id));

                allCourses.add(entry);
            }

            preparedStatement = connection.prepareStatement(selSelected);
            preparedStatement.setInt(1, semesterId);
            resultSet = preparedStatement.executeQuery();
            List<String> selectedCourses = new ArrayList<>();
            while (resultSet.next()) {
                selectedCourses.add(resultSet.getString("search_name"));
            }

            // close connection
            connection.close();
            preparedStatement.close();

            // add constraints
            if (searchCid != null) {
                totalEntries.removeIf(e -> searchCid.equals(e.course.id));
            }

            if (searchName != null) {
                totalEntries.removeIf(e -> searchName.equals(e.course.name + '[' + e.section.name + ']'));
            }

            List<CourseSearchEntry> temp = new ArrayList<>();
            if (searchInstructor != null) {
                for (CourseSearchEntry e : totalEntries) {
                    for (CourseSectionClass cls : e.sectionClasses) {
                        if (searchInstructor.equals(cls.instructor.fullName)) {
                            temp.add(e);
                            break;
                        }
                    }
                }
                totalEntries = temp;
            }

            temp = new ArrayList<>();
            if (searchDayOfWeek != null) {
                for (CourseSearchEntry e : totalEntries) {
                    for (CourseSectionClass cls : e.sectionClasses) {
                        if (searchDayOfWeek.equals(cls.dayOfWeek)) {
                            temp.add(e);
                            break;
                        }
                    }
                }
                totalEntries = temp;
            }

            temp = new ArrayList<>();
            if (searchClassTime != null) {
                for (CourseSearchEntry e : totalEntries) {
                    for (CourseSectionClass cls : e.sectionClasses) {
                        if (cls.classBegin <= searchClassTime && cls.classEnd >= searchClassTime) {
                            temp.add(e);
                            break;
                        }
                    }
                }
                totalEntries = temp;
            }

            temp = new ArrayList<>();
            if (searchClassLocations != null) {
                for (CourseSearchEntry e : totalEntries) {
                    for (CourseSectionClass cls : e.sectionClasses) {
                        if (searchClassLocations.contains(cls.location)) {
                            temp.add(e);
                            break;
                        }
                    }
                }
                totalEntries = temp;
            }

            if (ignoreFull) {
                totalEntries.removeIf(e -> e.section.leftCapacity == 0);
            }

            if (ignorePassed) {
                totalEntries.removeIf(e -> check_ALREADY_PASSED(studentId, e.section.id));
            }

            if (ignoreMissingPrerequisites) {
                totalEntries.removeIf(e -> !passedPrerequisitesForCourse(studentId, e.course.id));
            }

            for (CourseSearchEntry e : totalEntries) {
                List<String> conflict = new ArrayList<>();
                for (CourseSearchEntry target : allCourses) {
                    if (e.course.id.equals(target.course.id)) {
                        conflict.add(target.course.name + '[' + target.section.name + ']');
                    } else {
                        for (CourseSectionClass cls : e.sectionClasses) {
                            for (CourseSectionClass clsTarget : target.sectionClasses) {
                                for (Short week : cls.weekList) {
                                    for (Short targetWeek : clsTarget.weekList) {
                                        if (week.equals(targetWeek)) {
                                            if (cls.dayOfWeek.equals(clsTarget.dayOfWeek)) {
                                                if ((cls.classBegin >= clsTarget.classBegin && cls.classBegin <= clsTarget.classEnd)
                                                        || (cls.classEnd >= clsTarget.classBegin && cls.classEnd <= clsTarget.classEnd)
                                                        || (clsTarget.classBegin >= cls.classBegin && clsTarget.classBegin <= cls.classEnd)
                                                        || (clsTarget.classEnd >= cls.classBegin && clsTarget.classEnd <= cls.classEnd)) {
                                                    conflict.add(target.course.name + '[' + target.section.name + ']');
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                e.conflictCourseNames = conflict;
            }

            temp = new ArrayList<>();
            for (CourseSearchEntry e : totalEntries) {
                for (String s : selectedCourses) {
                    if (e.conflictCourseNames.contains(s)) {
                        temp.add(e);
                        break;
                    }
                }
            }
            totalEntries.removeAll(temp);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return totalEntries;
    }

    @Override
    public synchronized EnrollResult enrollCourse(int studentId, int sectionId) {

        String insertStudent = "insert into std_section values(?,?,null,null);";
        //COURSE_NOT_FOUND
        try {
            boolean if_COURSE_NOT_FOUND = check_COURSE_NOT_FOUND(sectionId);
            if(if_COURSE_NOT_FOUND){
                return  EnrollResult.COURSE_NOT_FOUND;
            }

            boolean if_ALREADY_ENROLLED = check_ALREADY_ENROLLED(studentId,sectionId);
            if(if_ALREADY_ENROLLED){
                return EnrollResult.ALREADY_ENROLLED;
            }
            boolean if_ALREADY_PASSED = check_ALREADY_PASSED(studentId,sectionId);
            if(if_ALREADY_PASSED){
                return EnrollResult.ALREADY_PASSED;
            }

            Connection connection = SQLDataSource.getInstance().getSQLConnection();
            PreparedStatement preparedStatement;
            ResultSet resultSet;

            String sectionToCourse = "select id_code from section\n" +
                    "join course c on section.course_id = c.course_id\n" +
                    "where section_id = ?";
            preparedStatement = connection.prepareStatement(sectionToCourse);
            preparedStatement.setInt(1,sectionId);
            resultSet =  preparedStatement.executeQuery();
            String courseId = "";
            while(resultSet.next()){
                courseId = resultSet.getString(1);
            }
            connection.close();
            preparedStatement.close();


            boolean if_PREREQUISITES_NOT_FULFILLED = passedPrerequisitesForCourse(studentId,courseId);
            if(!if_PREREQUISITES_NOT_FULFILLED){
                return EnrollResult.PREREQUISITES_NOT_FULFILLED;
            }

            boolean if_COURSE_CONFLICT_FOUND = check_COURSE_CONFLICT_FOUND(studentId, sectionId);
            if(if_COURSE_CONFLICT_FOUND){
                return EnrollResult.COURSE_CONFLICT_FOUND;
            }

            boolean if_COURSE_IS_FULL = check_COURSE_IS_FULL(sectionId);
            if(if_COURSE_IS_FULL){
                return EnrollResult.COURSE_IS_FULL;
            }

            //TODO:不知道这样子改写是不是对的
            Connection connection2 = SQLDataSource.getInstance().getSQLConnection();
            PreparedStatement preparedStatement2;


            preparedStatement2 = connection2.prepareStatement(insertStudent);
            preparedStatement2.setInt(1,studentId);
            preparedStatement2.setInt(2,sectionId);
            preparedStatement2.execute();

            connection2.close();
            preparedStatement2.close();

            // close connection

        } catch (SQLException e) {
            return EnrollResult.UNKNOWN_ERROR;
        }

        return EnrollResult.SUCCESS;
    }

    @Override
    public void dropCourse(int studentId, int sectionId) throws IllegalStateException {
        //TODO：这里的try catch具体怎么完成需要理解
        //从学生已经选择的课程中删去一门课，如果这门课已经有成绩了，那么不能删除，需要抛出异常
        String check = "select ((select score from std_section where std_id = ? and section_id = ?) is null);";
        String sql = "delete from std_section where std_id = ? and section_id = ? and score is null";
        PreparedStatement preparedStatement;
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();

            preparedStatement = connection.prepareStatement(check);
            preparedStatement.setInt(1, studentId);
            preparedStatement.setInt(2, sectionId);
            ResultSet resultSet;
            resultSet = preparedStatement.executeQuery();
            boolean judgeCheck = false;
            while(resultSet.next()){
                judgeCheck = resultSet.getBoolean(1);
            }
            connection.close();
            preparedStatement.close();




            if(!judgeCheck){
                //说明其不为空
                throw new IllegalStateException();
            }
            else{
                Connection connection2 = SQLDataSource.getInstance().getSQLConnection();
                PreparedStatement preparedStatement2;
                // add to course table not handling pre
                preparedStatement2 = connection2.prepareStatement(sql);
                preparedStatement2.setInt(1, studentId);
                preparedStatement2.setInt(2, sectionId);
                preparedStatement2.execute();

                // close connection
                connection2.close();
                preparedStatement2.close();
            }


        } catch (SQLException e) {
            //这里的异常怎么抛出需要理解
            e.printStackTrace();
        }


    }
    @Override
    public void addEnrolledCourseWithGrade(int studentId, int sectionId, @Nullable Grade grade) {
        //绕过先修课，直接给一门课程来进行打分操作
        String sql = "insert into std_section values (?,?,?,?)";
        PreparedStatement preparedStatement;
        try {

            Connection connection = SQLDataSource.getInstance().getSQLConnection();
            // add to course table not handling pre
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, studentId);
            preparedStatement.setInt(2, sectionId);

            int gradeScore;
            boolean gradeType;

            if(grade == null){
                preparedStatement.setObject(3,null);
                preparedStatement.setObject(4,null);
            }
            else{
                if (grade instanceof HundredMarkGrade) {
                    HundredMarkGrade temp = (HundredMarkGrade) grade;
                    gradeScore = temp.mark;
                    gradeType = true;
                } else {
                    PassOrFailGrade temp = (PassOrFailGrade) grade;
                    if (temp.equals(PassOrFailGrade.PASS)) {
                        gradeScore = 1;
                        gradeType = false;
                    } else {
                        gradeScore = 0;
                        gradeType = false;
                    }
                }

                preparedStatement.setInt(3, gradeScore);
                preparedStatement.setBoolean(4, gradeType);
            }



            preparedStatement.execute();

            // close connection
            connection.close();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void setEnrolledCourseGrade(int studentId, int sectionId, Grade grade) {
        String sql = "insert into std_section values (?,?,?,?)";
        PreparedStatement preparedStatement;
        try {

            Connection connection = SQLDataSource.getInstance().getSQLConnection();
            // add to course table not handling pre
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, studentId);
            preparedStatement.setInt(2, sectionId);

            int gradeScore;
            boolean gradeType;


            if (grade instanceof HundredMarkGrade) {
                HundredMarkGrade temp = (HundredMarkGrade) grade;
                gradeScore = temp.mark;
                gradeType = true;
            } else {
                PassOrFailGrade temp = (PassOrFailGrade) grade;
                if (temp.equals(PassOrFailGrade.PASS)) {
                    gradeScore = 1;
                    gradeType = false;
                } else {
                    gradeScore = 0;
                    gradeType = false;
                }
            }

            preparedStatement.setInt(3, gradeScore);
            preparedStatement.setBoolean(4, gradeType);

            preparedStatement.execute();

            // close connection
            connection.close();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Map<Course, Grade> getEnrolledCoursesAndGrades(int studentId, @Nullable Integer semesterId) {
        //查询学生在一个学期内所有课程的乘成绩
//        Grade a = new HundredMarkGrade((short)1);
//        Grade b =  PassOrFailGrade.PASS;
        Map<Course, Grade> courseGradeInformationMap = new HashMap<>();

        String getAllInformationOfCG = "select id_code,c.name,credit,class_hour,grading,score,type from std_section\n" +
                "join section_semester ss on std_section.section_id = ss.section_id\n" +
                "join section s on ss.section_id = s.section_id\n" +
                "join course c on s.course_id = c.course_id\n" +
                "where std_id = ? and semester_id = ?;";

        String getAllInformationOfCGNullSemester = "select id_code,c.name,credit,class_hour,grading,score,type from std_section\n" +
                "join section_semester ss on std_section.section_id = ss.section_id\n" +
                "join section s on ss.section_id = s.section_id\n" +
                "join course c on s.course_id = c.course_id\n" +
                "where std_id = ?;";
        PreparedStatement preparedStatement;
        try {

            Connection connection = SQLDataSource.getInstance().getSQLConnection();

            ResultSet getAllInformationOfCG_result;

            if(semesterId == null){
                preparedStatement = connection.prepareStatement(getAllInformationOfCGNullSemester);
                preparedStatement.setInt(1, studentId);
                getAllInformationOfCG_result = preparedStatement.executeQuery();
            }
            else{
                preparedStatement = connection.prepareStatement(getAllInformationOfCG);
                preparedStatement.setInt(1, studentId);
                preparedStatement.setInt(2, semesterId);
                getAllInformationOfCG_result = preparedStatement.executeQuery();
            }

            // add to course table not handling pre
            while (getAllInformationOfCG_result.next()) {
                //进行循环取出里面
                Grade tempGrade;
                Course tempCourse = new Course();
                if (getAllInformationOfCG_result.getInt(7) == 0) {
                    //说明现在是通过/不通过
                    if (getAllInformationOfCG_result.getInt(6) == 0) {
                        tempGrade = PassOrFailGrade.FAIL;
                    } else {
                        tempGrade = PassOrFailGrade.PASS;
                    }
                } else {
                    //否则按照百分制来给成绩
                    tempGrade = new HundredMarkGrade((short) getAllInformationOfCG_result.getInt(6));
                }

                tempCourse.id = getAllInformationOfCG_result.getString(1);
                tempCourse.name = getAllInformationOfCG_result.getString(2);
                tempCourse.credit = getAllInformationOfCG_result.getInt(3);
                tempCourse.classHour = getAllInformationOfCG_result.getInt(4);
                if (getAllInformationOfCG_result.getInt(5) == 0) {
                    tempCourse.grading = Course.CourseGrading.PASS_OR_FAIL;
                } else {
                    tempCourse.grading = Course.CourseGrading.HUNDRED_MARK_SCORE;
                }
                courseGradeInformationMap.put(tempCourse, tempGrade);
            }

            // close connection
            connection.close();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return courseGradeInformationMap;
    }


    @Override
    public CourseTable getCourseTable(int studentId, Date date) {
        //查询学生在当前周次的课程表
        //需要使用除法来计算出当前的周次
        //首先要获取出来整个的日期范围
        //需要获取到所有的日期，然后来进行比较
        SemesterServiceIns semesterServiceIns = new SemesterServiceIns();
        List<Semester> allSemesterList = semesterServiceIns.getAllSemesters();

        //TODO:这里semester的值是怎么得到的还不知道（问题还没有解决）

        CourseTable getCourseTableElement = new CourseTable();

        int semesterId = 0;
        long weekth = 0;
        for (Semester semester : allSemesterList) {
            //开始进行循环
            Date tempDateBefore = semester.begin;
            int beforeCompare = tempDateBefore.compareTo(date);
            Date tempDateEnd = semester.end;
            int afterCompare = tempDateEnd.compareTo(date);
            if (beforeCompare >= 0 && afterCompare <= 0) {
                //说明传入的日期在这两个学期中间
                //需要返回整个周次的课程表
                //需要知道当前是哪个学期和相应的周次信息
                //这个date的值直接会告诉是那一天
                semesterId = semester.id; //得到有关周次的相关信息

                long timeDis = Math.abs(date.getTime() - semester.begin.getTime());//获取到天数，然后利用天数来计算周次
                long day = timeDis / (1000 * 60 * 60 * 24);
                weekth = (day / 7) + 1; //weekth表示当前的周次是第几周
                //TODO:这里的计算可能出现问题
                //根据这个周次来继续去找
                break;
            }
        }

        //std_section section_semester
        //studentId semester week
        //day of week 来进行循环操作
        //小班也要起同一个名字
        //还要知道course的名字
        
        //现在获取到了周次需要根据周次来找其上的课程

        String allCourseTableEntry = "select c2.id_code,s.name,i.ins_id,i.full_name,c.class_begin,c.class_end,c.location,c.day_of_week from std_section\n" +
                "join section_semester ss on std_section.section_id = ss.section_id\n" +
                "join class c on ss.section_id = c.section_id\n" +
                "join week_class wc on c.class_id = wc.class_id\n" +
                "join section s on s.section_id = c.section_id\n" +
                "join course c2 on s.course_id = c2.course_id\n" +
                "join instructor i on c.instructor_id = i.ins_id\n" +
                "where std_section.std_id = ?\n" +
                "and ss.semester_id = ?\n" +
                "and wc.week = ?\n" +
                "order by day_of_week asc;";

        PreparedStatement preparedStatement;
        ResultSet resultSet;
        Map<DayOfWeek, Set<CourseTable.CourseTableEntry>> courseTable = new HashMap<>();
        CourseTable courseTableResult = new CourseTable();
        courseTableResult.table =courseTable;

        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();
            preparedStatement = connection.prepareStatement(allCourseTableEntry);
            preparedStatement.setInt(1,studentId);
            preparedStatement.setInt(2,semesterId);
            preparedStatement.setInt(3, (int) weekth);
            resultSet = preparedStatement.executeQuery();
            int judgeWeek = 1;


            Set<CourseTable.CourseTableEntry> tempCourseTableEntrySet= new HashSet<>();
            while(resultSet.next()){
                int judge_day_of_week = resultSet.getInt(8);//这个可以判断出当前的周次是什么
                if(judge_day_of_week == judgeWeek){
                    //这个条件满足说明在当前情况下周次没有发生改变
                    CourseTable.CourseTableEntry tempCourseTableEntry = new CourseTable.CourseTableEntry();
                    Instructor tempInstructor = new Instructor();
                    String courseNameBefore = resultSet.getString(1);
                    String courseNameAfter = resultSet.getString(2);
                    String courseFullName = courseNameBefore + "[" + courseNameAfter + "]";
                    tempInstructor.id = resultSet.getInt(3);
                    tempInstructor.fullName = resultSet.getString(4);


                    tempCourseTableEntry.courseFullName = courseFullName;
                    tempCourseTableEntry.instructor = tempInstructor;
                    tempCourseTableEntry.classBegin = (short) resultSet.getInt(5);
                    tempCourseTableEntry.classEnd = (short) resultSet.getInt(6);
                    tempCourseTableEntry.location = resultSet.getString(7);

                    tempCourseTableEntrySet.add(tempCourseTableEntry);//加入这一节课程
                }
                else{

                    DayOfWeek tempWeek = DayOfWeek.of(judgeWeek);
                    courseTable.put(tempWeek,tempCourseTableEntrySet);

                    tempCourseTableEntrySet = new HashSet<>();
                    judgeWeek++;

                }
            }

            connection.close();
            preparedStatement.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return courseTableResult;
    }

    public synchronized boolean check_COURSE_NOT_FOUND(int sectionId){
        PreparedStatement preparedStatement;
        boolean judge = false ;
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();

            String check_if_COURSE_NOT_FOUND = "select exists(select section_id from section where section_id = ? )";
            preparedStatement = connection.prepareStatement(check_if_COURSE_NOT_FOUND);
            preparedStatement.setInt(1, sectionId);
            ResultSet check_if_COURSE_NOT_FOUND_judge = preparedStatement.executeQuery();

            while(check_if_COURSE_NOT_FOUND_judge.next()){
                if (!check_if_COURSE_NOT_FOUND_judge.getBoolean(1)) {
                    judge = true;
                }
            }

            connection.close();
            preparedStatement.close();


        } catch (SQLException e) {
            e.printStackTrace();
        }
        return judge;     //return EnrollResult.COURSE_NOT_FOUND;
    }

    public synchronized boolean check_COURSE_IS_FULL(int sectionId){
        PreparedStatement preparedStatement;
        boolean judge = false ;
        ResultSet resultSet;
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();

            String check_if_COURSE_IS_FULL = "select left_capacity from section where section_id = ?";
            preparedStatement = connection.prepareStatement(check_if_COURSE_IS_FULL);
            preparedStatement.setInt(1, sectionId);
            resultSet = preparedStatement.executeQuery();
            int left_capacity = 0;
            while(resultSet.next()){
                left_capacity =  resultSet.getInt(1);
            }
            judge = left_capacity == 0;

            connection.close();
            preparedStatement.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return judge;     //return EnrollResult.COURSE_IS_FULL;
    }

    public synchronized boolean check_ALREADY_ENROLLED(int studentId,int sectionId){
        PreparedStatement preparedStatement;
        boolean judge = false ;
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();
            String check_if_ALREADY_ENROLLED = "select std_section.std_id,std_section.section_id,std_section.type from std_section where std_id = ? and section_id = ?";
            preparedStatement = connection.prepareStatement(check_if_ALREADY_ENROLLED);
            preparedStatement.setInt(1, studentId);
            preparedStatement.setInt(2, sectionId);
            ResultSet check_if_ALREADY_ENROLLED_judge = preparedStatement.executeQuery();
            while(check_if_ALREADY_ENROLLED_judge.next()){
                if (check_if_ALREADY_ENROLLED_judge.getObject(1) != null && check_if_ALREADY_ENROLLED_judge.getObject(3) != null) { //第三列为type
                    //说明这门课程是这学期选的
                    judge = true;

                }
            }
            connection.close();
            preparedStatement.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return judge;     //return EnrollResult.ALREADY_ENROLLED;
    }


    public synchronized boolean check_ALREADY_PASSED(int studentId,int sectionId){
        PreparedStatement preparedStatement;
        boolean judge = false ;
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();
            String check_if_ALREADY_PASSED = "select std_section.std_id,std_section.section_id,std_section.score,std_section.type from std_section where std_id = ? and section_id = ?";
            preparedStatement = connection.prepareStatement(check_if_ALREADY_PASSED);
            preparedStatement.setInt(1, studentId);
            preparedStatement.setInt(2, sectionId);
            ResultSet check_if_ALREADY_PASSED_judge = preparedStatement.executeQuery();
            while (check_if_ALREADY_PASSED_judge.next()) {
                if (check_if_ALREADY_PASSED_judge.getObject(1) != null && check_if_ALREADY_PASSED_judge.getObject(4) != null) {
                    if (!check_if_ALREADY_PASSED_judge.getBoolean(4)) {//表示通过、不通过
                        if (check_if_ALREADY_PASSED_judge.getInt(3) == 1) {
                            judge = true;
                        }
                    } else {
                        if (check_if_ALREADY_PASSED_judge.getInt(3) >= 60) {
                            judge = true;
                        }
                    }
                }
            }

            connection.close();
            preparedStatement.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return judge;     //false表示这门课并不存在 return EnrollResult.ALREADY_PASSED;
    }



    @Override
    public synchronized boolean passedPrerequisitesForCourse(int studentId, String courseId) { //这里的course_id是id_code
        //需要由这个courseid得到course对应的编号才行
        int courseId_num = 0;
        String find_courseId_num = "select course_id from course where id_code = ?";
        PreparedStatement preparedStatement;
        int if_satisfy_pre = 0;
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();

            preparedStatement = connection.prepareStatement(find_courseId_num);
            preparedStatement.setString(1, courseId);
            ResultSet find_courseId_num_result = preparedStatement.executeQuery();
            while (find_courseId_num_result.next()) {
                courseId_num = find_courseId_num_result.getInt(1);
            }

            //需要有一张专门的表来记录这名学生这门课程的具体选中情况
            String PREREQUISITES_group = "select count(*) from prerequisite where course_id = ? ";//PREREQUISITES_group 知道这门课程有多少个先修课组
            preparedStatement = connection.prepareStatement(PREREQUISITES_group);
            preparedStatement.setInt(1, courseId_num);
            ResultSet PREREQUISITES_group_result = preparedStatement.executeQuery();

            int PREREQUISITES_group_count = 0;

            while(PREREQUISITES_group_result.next()){
                PREREQUISITES_group_count = PREREQUISITES_group_result.getInt(1);
            }


            for (int i = 1; i <= PREREQUISITES_group_count; i++) {
                int count_paseed_course = 0;//检查每组先修组合下面已经通过的课程数量
                String PREREQUISITES_small_group_number = "select count(*) from prerequisite where course_id = ? and and_group = ?";//PREREQUISITES_small_group知道每个先修课组合下面有多少门课程
                preparedStatement = connection.prepareStatement(PREREQUISITES_small_group_number);
                preparedStatement.setInt(1, courseId_num);
                preparedStatement.setInt(2, i);
                ResultSet PREREQUISITES_small_group_number_result = preparedStatement.executeQuery(); //这个表示查询返回的集合
                int should_paseed_course = 0;
                while(PREREQUISITES_small_group_number_result.next()){
                    should_paseed_course = PREREQUISITES_small_group_number_result.getInt(1);
                }
                String PREREQUISITES_small_group = "select pre_id from prerequisite where course_id = ? and and_group = ?";//PREREQUISITES_small_group知道每个先修课组合下面有多少门课程
                preparedStatement = connection.prepareStatement(PREREQUISITES_small_group);
                preparedStatement.setInt(1, courseId_num);
                preparedStatement.setInt(2, i);
                ResultSet PREREQUISITES_small_group_result = preparedStatement.executeQuery(); //这个表示查询返回的集合
                while (PREREQUISITES_small_group_result.next()) {
                    int PREREQUISITES_small_group_pre_id = PREREQUISITES_small_group_result.getInt(1); //得到当前想查询的pre_id是多少
                    String PREREQUISITES_check_if_pass = "select std_section.section_id,std_section.score,std_section.type from std_section where std_id = ? and section_id = ?";
                    preparedStatement = connection.prepareStatement(PREREQUISITES_check_if_pass);
                    preparedStatement.setInt(1, studentId);
                    preparedStatement.setInt(2, PREREQUISITES_small_group_pre_id);
                    ResultSet PREREQUISITES_check_if_pass_end = preparedStatement.executeQuery(); //得到返回的结果，section_id 不为空，type不为空，同时成绩及格才能说明先修课满足
                    while(PREREQUISITES_check_if_pass_end.next()){
                        if (PREREQUISITES_check_if_pass_end.getObject(1) != null && PREREQUISITES_check_if_pass_end.getObject(3) != null) {
                            if (!PREREQUISITES_check_if_pass_end.getBoolean(3)) {//表示通过、不通过
                                if (PREREQUISITES_check_if_pass_end.getInt(2) == 1) {
                                    count_paseed_course++;
                                }
                            } else {
                                if (PREREQUISITES_check_if_pass_end.getInt(2) >= 60) {
                                    count_paseed_course++;
                                }
                            }
                        }
                    }
                }
                if (count_paseed_course == should_paseed_course) {
                    if_satisfy_pre++;
                }
            }

            connection.close();
            preparedStatement.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }
        //等于0说明循环下来没有满足先修关系的课程
        return if_satisfy_pre != 0;
    }

    public synchronized boolean check_COURSE_CONFLICT_FOUND(int studentId, int sectionId){
        String selSelected = "select s.section_id,\n" +
                "       c.course_id,\n" +
                "       s2.name                           as section,\n" +
                "       total_capacity,\n" +
                "       left_capacity,\n" +
                "       id_code,\n" +
                "       c.name                           as course,\n" +
                "       credit,\n" +
                "       class_hour,\n" +
                "       grading,\n" +
                "       (c.name || '[' || s2.name || ']') as search_name\n" +
                "from section_semester ss\n" +
                "         join std_section s on ss.section_id = s.section_id\n" +
                "         join section s2 on s2.section_id = s.section_id\n" +
                "         join course c on c.course_id = s2.course_id\n" +
                "where std_id = ?\n" +
                "  and semester_id = (\n" +
                "    select semester_id\n" +
                "    from section_semester\n" +
                "    where section_id = ?);";

        PreparedStatement preparedStatement;
        ResultSet resultSet;
        boolean conflict = false;
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();

            CourseService courseService = new CourseServiceIns();

            preparedStatement = connection.prepareStatement(selSelected);
            preparedStatement.setInt(1, studentId);
            preparedStatement.setInt(2, sectionId);
            resultSet = preparedStatement.executeQuery();
            CourseSection section;
            List<CourseSearchEntry> searchEntries = new ArrayList<>();
            while (resultSet.next()) {
                CourseSearchEntry entry = new CourseSearchEntry();

                Course course = new Course();
                course.id = resultSet.getString("id_code");
                course.name = resultSet.getString("course");
                course.grading = Course.CourseGrading.values()[resultSet.getBoolean("grading") ? 1 : 0];
                course.credit = resultSet.getInt("credit");
                course.classHour = resultSet.getInt("class_hour");
                entry.course = course;

                section = new CourseSection();
                section.id = resultSet.getInt("section_id");
                section.name = resultSet.getString("section");
                section.totalCapacity = resultSet.getInt("total_capacity");
                section.leftCapacity = resultSet.getInt("left_capacity");
                entry.section = section;

                entry.sectionClasses = new HashSet<>();
                entry.sectionClasses.addAll(courseService.getCourseSectionClasses(section.id));

                searchEntries.add(entry);
            }
            preparedStatement.close();
            connection.close();

            CourseSearchEntry self = new CourseSearchEntry();
            self.course = courseService.getCourseBySection(sectionId);
            self.sectionClasses = new HashSet<>();
            self.sectionClasses.addAll(courseService.getCourseSectionClasses(sectionId));

            for (CourseSearchEntry e : searchEntries) {
                if (e.section.id == sectionId) {
                    conflict = true;
                    break;
                } else {
                    for (CourseSectionClass cls : e.sectionClasses) {
                        for (CourseSectionClass clsTarget : self.sectionClasses) {
                            for (Short week : cls.weekList) {
                                for (Short targetWeek : clsTarget.weekList) {
                                    if (week.equals(targetWeek)) {
                                        if (cls.dayOfWeek.equals(clsTarget.dayOfWeek)) {
                                            if ((cls.classBegin >= clsTarget.classBegin && cls.classBegin <= clsTarget.classEnd)
                                                    || (cls.classEnd >= clsTarget.classBegin && cls.classEnd <= clsTarget.classEnd)
                                                    || (clsTarget.classBegin >= cls.classBegin && clsTarget.classBegin <= cls.classEnd)
                                                    || (clsTarget.classEnd >= cls.classBegin && clsTarget.classEnd <= cls.classEnd)) {
                                                conflict = true;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return conflict;
    }

    @Override
    public Major getStudentMajor(int studentId) {
        String getMajor = "select *\n" +
                "from student s\n" +
                "         join major m on s.major_id = m.major_id\n" +
                "where s.major_id = ?;";
        PreparedStatement preparedStatement;
        ResultSet resultSet;
        Major major = new Major();
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();

            // add to course table not handling pre
            preparedStatement = connection.prepareStatement(getMajor);
            preparedStatement.setInt(1, studentId);
            resultSet = preparedStatement.executeQuery();
            major.id = resultSet.getInt("s.major_id");
            major.name = resultSet.getString("name");
            DepartmentServiceIns departmentServiceIns = new DepartmentServiceIns();
            major.department = departmentServiceIns.getDepartment(resultSet.getInt("department_id"));

            // close connection
            connection.close();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return major;
    }
}
