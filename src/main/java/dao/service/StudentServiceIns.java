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
        //这个方法最后再写
        return null;
    }

    @Override
    public EnrollResult enrollCourse(int studentId, int sectionId) {
        //TODO:现在默认courseID和sectionID相等，之后还要进行修改
        PreparedStatement preparedStatement;
        //COURSE_NOT_FOUND
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();

            String check_if_COURSE_NOT_FOUND = "select exists(select section_id from section where section_id = ? )";
            preparedStatement = connection.prepareStatement(check_if_COURSE_NOT_FOUND);
            preparedStatement.setInt(1, sectionId);
            ResultSet check_if_COURSE_NOT_FOUND_judge = preparedStatement.executeQuery();
            if (check_if_COURSE_NOT_FOUND_judge.getBoolean(1) == false) {
                //说明这门课程不存在
                return EnrollResult.COURSE_NOT_FOUND;
            }
            //完成对课程是否存在的判断
            //接下来判断ALREADY_ENROLLED
            //课程已经进入但是没有成绩的情况,如果type为空，说明这个学生已经选课但是成绩没有录入
            String check_if_ALREADY_ENROLLED = "select std_section.std_id,std_section.section_id,std_section.type from std_section where std_id = ? and section_id = ?";
            preparedStatement = connection.prepareStatement(check_if_ALREADY_ENROLLED);
            preparedStatement.setInt(1, studentId);
            preparedStatement.setInt(2, sectionId);
            ResultSet check_if_ALREADY_ENROLLED_judge = preparedStatement.executeQuery();
            if (check_if_ALREADY_ENROLLED_judge.getObject(1) != null && check_if_ALREADY_ENROLLED_judge.getObject(3) != null) { //第三列为type
                //说明这门课程是这学期选的
                return EnrollResult.ALREADY_ENROLLED;
            }
            //ALREADY_ENROLLED判断完成
            //接下来判断ALREADY_PASSED
            String check_if_ALREADY_PASSED = "select std_section.std_id,std_section.section_id,std_section.score,std_section.type from std_section where std_id = ? and section_id = ?";
            preparedStatement = connection.prepareStatement(check_if_ALREADY_PASSED);
            preparedStatement.setInt(1, studentId);
            preparedStatement.setInt(2, sectionId);
            ResultSet check_if_ALREADY_PASSED_judge = preparedStatement.executeQuery();
            if (check_if_ALREADY_PASSED_judge.getObject(1) != null && check_if_ALREADY_PASSED_judge.getObject(4) != null) {
                if (check_if_ALREADY_PASSED_judge.getBoolean(4) == false) {//表示通过、不通过
                    if (check_if_ALREADY_PASSED_judge.getInt(3) == 1) {
                        return EnrollResult.ALREADY_PASSED;
                    }
                } else {
                    if (check_if_ALREADY_PASSED_judge.getInt(3) >= 60) {
                        return EnrollResult.ALREADY_PASSED;
                    }
                }
            }
            //接下里判断先修课关系
            //check_if_PREREQUISITES_NOT_FULFILLED
            //TODO 这里各个id之间的关系需要进行修改
            String PREREQUISITES_group = "select count(*) from prerequisite where course_id = ? ";//PREREQUISITES_group 知道这门课程有多少个先修课组
            preparedStatement = connection.prepareStatement(PREREQUISITES_group);
            preparedStatement.setInt(1, sectionId);
            ResultSet PREREQUISITES_group_result = preparedStatement.executeQuery();
            int if_satisfy_pre = 0;
            int PREREQUISITES_group_count = PREREQUISITES_group_result.getInt(1);
            for (int i = 1; i <= PREREQUISITES_group_count; i++) {
                int count_paseed_course = 0;//检查每组先修组合下面已经通过的课程数量
                String PREREQUISITES_small_group_number = "select count(*) from prerequisite where course_id = ? and and_group = ?";//PREREQUISITES_small_group知道每个先修课组合下面有多少门课程
                preparedStatement = connection.prepareStatement(PREREQUISITES_small_group_number);
                preparedStatement.setInt(1, sectionId);
                preparedStatement.setInt(2, i);
                ResultSet PREREQUISITES_small_group_number_result = preparedStatement.executeQuery(); //这个表示查询返回的集合
                int should_paseed_course = PREREQUISITES_small_group_number_result.getInt(1);
                String PREREQUISITES_small_group = "select pre_id from prerequisite where course_id = ? and and_group = ?";//PREREQUISITES_small_group知道每个先修课组合下面有多少门课程
                preparedStatement = connection.prepareStatement(PREREQUISITES_small_group);
                preparedStatement.setInt(1, sectionId);
                preparedStatement.setInt(2, i);
                ResultSet PREREQUISITES_small_group_result = preparedStatement.executeQuery(); //这个表示查询返回的集合
                while (PREREQUISITES_small_group_result.next()) {
                    int PREREQUISITES_small_group_pre_id = PREREQUISITES_small_group_result.getInt(1); //得到当前想查询的pre_id是多少
                    String PREREQUISITES_check_if_pass = "select std_section.section_id,std_section.score,std_section.type from std_section where std_id = ? and section_id = ?";
                    preparedStatement = connection.prepareStatement(PREREQUISITES_check_if_pass);
                    preparedStatement.setInt(1, studentId);
                    preparedStatement.setInt(2, PREREQUISITES_small_group_pre_id);
                    ResultSet PREREQUISITES_check_if_pass_end = preparedStatement.executeQuery(); //得到返回的结果，section_id 不为空，type不为空，同时成绩及格才能说明先修课满足
                    if (PREREQUISITES_check_if_pass_end.getObject(1) != null && PREREQUISITES_check_if_pass_end.getObject(3) != null) {
                        if (PREREQUISITES_check_if_pass_end.getBoolean(3) == false) {//表示通过、不通过
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
                if (count_paseed_course == should_paseed_course) {
                    if_satisfy_pre++;
                }

            }

            if (if_satisfy_pre == 0) { //等于0说明循环下来没有满足先修关系的课程
                return EnrollResult.PREREQUISITES_NOT_FULFILLED;
            }
            //完成对是否满足先修课关系的判断

            //通过sectionid可以唯一地查到courseid是什么


            // close connection
            connection.close();
            preparedStatement.close();
        } catch (SQLException e) {
            throw new IntegrityViolationException();
        }

        //处理学生选什么课问题：
        //注意：这里要考虑先修课条件是否满足，先修课条件满足才能进行选课操作
        //TODO:it is just a test
        return EnrollResult.COURSE_NOT_FOUND;
    }

    @Override
    public void dropCourse(int studentId, int sectionId) throws IllegalStateException {
        //TODO：这里的try catch具体怎么完成需要理解
        //从学生已经选择的课程中删去一门课，如果这门课已经有成绩了，那么不能删除，需要抛出异常
        String sql = "delete from std_section where std_id = ? and section_id = ?";
        PreparedStatement preparedStatement;
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();

            // add to course table not handling pre
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, studentId);
            preparedStatement.setInt(2, sectionId);
            preparedStatement.execute();

            // close connection
            connection.close();
            preparedStatement.close();
        } catch (SQLException e) {
            //这里的异常怎么抛出需要理解
            throw new IllegalStateException();
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
            int gradeType;

            //TODO：注意：这里的成绩可以为空，目前还没有做成绩为空的后续处理

            if (grade instanceof HundredMarkGrade) {
                HundredMarkGrade temp = (HundredMarkGrade) grade;
                gradeScore = temp.mark;
                gradeType = 1;
            } else {
                PassOrFailGrade temp = (PassOrFailGrade) grade;
                if (temp.equals(PassOrFailGrade.PASS)) {
                    gradeScore = 1;
                    gradeType = 0;
                } else {
                    gradeScore = 0;
                    gradeType = 1;
                }
            }

            preparedStatement.setInt(3, gradeScore);
            preparedStatement.setInt(4, gradeType);


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
            int gradeType;

            //TODO：注意：这里的成绩可以为空，目前还没有做成绩为空的后续处理

            if (grade instanceof HundredMarkGrade) {
                HundredMarkGrade temp = (HundredMarkGrade) grade;
                gradeScore = temp.mark;
                gradeType = 1;
            } else {
                PassOrFailGrade temp = (PassOrFailGrade) grade;
                if (temp.equals(PassOrFailGrade.PASS)) {
                    gradeScore = 1;
                    gradeType = 0;
                } else {
                    gradeScore = 0;
                    gradeType = 1;
                }
            }

            preparedStatement.setInt(3, gradeScore);
            preparedStatement.setInt(4, gradeType);


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
        //TODO：这里的semester id 可以为空还没有进行处理
        //查询学生在一个学期内所有课程的乘成绩
//        Grade a = new HundredMarkGrade((short)1);
//        Grade b =  PassOrFailGrade.PASS;
        Map<Course, Grade> courseGradeInformationMap = new HashMap<>();
        //完成对于枚举类的定义工作
        String getAllInformationOfCG = "select id_code,c.name,credit,class_hour,grading,score,type from std_section\n" +
                "join section_semester ss on std_section.section_id = ss.section_id\n" +
                "join section s on ss.section_id = s.section_id\n" +
                "join course c on s.course_id = c.course_id\n" +
                "where std_id = ? and semester_id = ?;";
        PreparedStatement preparedStatement;
        try {

            Connection connection = SQLDataSource.getInstance().getSQLConnection();
            // add to course table not handling pre
            preparedStatement = connection.prepareStatement(getAllInformationOfCG);
            preparedStatement.setInt(1, studentId);
            preparedStatement.setInt(2, semesterId);
            ResultSet getAllInformationOfCG_result = preparedStatement.executeQuery();
            while (getAllInformationOfCG_result.next()) {
                //TODO:需要加一个 成绩是否为空的判断
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


        //需要join表

        //首先要找到所有的section，然后再把成绩反映出来

        //需要将其实例化到grade中然后加进去
        return courseGradeInformationMap;
    }


    // TODO:
    @Override
    public CourseTable getCourseTable(int studentId, Date date) {
        //查询学生在当前周次的课程表
        //需要使用除法来计算出当前的周次
        //首先要获取出来整个的日期范围
        //需要获取到所有的日期，然后来进行比较
        List<Semester> allSemesterList = null;
        //TODO:这里semester的值是怎么得到的还不知道（问题还没有解决）

        CourseTable getCourseTableElement = new CourseTable();

        int semesterId;
        long weekth;
        for (int i = 0; i < allSemesterList.size(); i++) {
            //开始进行循环
            Date tempDateBefore = allSemesterList.get(i).begin;
            int beforeCompare = tempDateBefore.compareTo(date);
            Date tempDateEnd = allSemesterList.get(i).end;
            int afterCompare = tempDateEnd.compareTo(date);
            if (beforeCompare >= 0 && afterCompare <= 0) {
                //说明传入的日期在这两个学期中间
                //需要返回整个周次的课程表
                //需要知道当前是哪个学期和相应的周次信息
                //这个date的值直接会告诉是那一天
                semesterId = allSemesterList.get(i).id; //得到有关周次的相关信息

                long timeDis = Math.abs(date.getTime() - allSemesterList.get(i).begin.getTime());//获取到天数，然后利用天数来计算周次
                long day = timeDis / (1000 * 60 * 60 * 24);
                weekth = (day / 7) + 1; //weekth表示当前的周次是第几周
                //TODO:这里的计算可能出现问题
                //根据这个周次来继续去找
                break;
            }
        }

        String sectionSet = "select course_id from course where id_code = ?";
        PreparedStatement preparedStatement;
        ResultSet resultSet;
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();
            preparedStatement = connection.prepareStatement(sectionSet);


        } catch (SQLException e) {
            e.printStackTrace();
        }

        //接下来做数据库相应的查询工作来完成后续的内容

        //首先先得出来所有section的集合,然后再添加小班的名称


        CourseTable.CourseTableEntry test = new CourseTable.CourseTableEntry();


        return null;
    }

    @Override
    public boolean passedPrerequisitesForCourse(int studentId, String courseId) { //这里的course_id是id_code
        //需要由这个courseid得到course对应的编号才行
        int courseId_num;
        String find_courseId_num = "select course_id from course where id_code = ?";
        PreparedStatement preparedStatement;
        int if_satisfy_pre = 0;
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();

            preparedStatement = connection.prepareStatement(find_courseId_num);
            preparedStatement.setString(1, courseId);
            ResultSet find_courseId_num_result = preparedStatement.executeQuery();
            courseId_num = find_courseId_num_result.getInt(1);

            //需要有一张专门的表来记录这名学生这门课程的具体选中情况
            String PREREQUISITES_group = "select count(*) from prerequisite where course_id = ? ";//PREREQUISITES_group 知道这门课程有多少个先修课组
            preparedStatement = connection.prepareStatement(PREREQUISITES_group);
            preparedStatement.setInt(1, courseId_num);
            ResultSet PREREQUISITES_group_result = preparedStatement.executeQuery();

            int PREREQUISITES_group_count = PREREQUISITES_group_result.getInt(1);
            for (int i = 1; i <= PREREQUISITES_group_count; i++) {
                int count_paseed_course = 0;//检查每组先修组合下面已经通过的课程数量
                String PREREQUISITES_small_group_number = "select count(*) from prerequisite where course_id = ? and and_group = ?";//PREREQUISITES_small_group知道每个先修课组合下面有多少门课程
                preparedStatement = connection.prepareStatement(PREREQUISITES_small_group_number);
                preparedStatement.setInt(1, courseId_num);
                preparedStatement.setInt(2, i);
                ResultSet PREREQUISITES_small_group_number_result = preparedStatement.executeQuery(); //这个表示查询返回的集合
                int should_paseed_course = PREREQUISITES_small_group_number_result.getInt(1);
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
                    if (PREREQUISITES_check_if_pass_end.getObject(1) != null && PREREQUISITES_check_if_pass_end.getObject(3) != null) {
                        if (PREREQUISITES_check_if_pass_end.getBoolean(3) == false) {//表示通过、不通过
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
                if (count_paseed_course == should_paseed_course) {
                    if_satisfy_pre++;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (if_satisfy_pre == 0) { //等于0说明循环下来没有满足先修关系的课程
            return false;
        } else {
            return true;
        }
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
