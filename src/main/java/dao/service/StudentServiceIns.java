package dao.service;
import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.*;
import cn.edu.sustech.cs307.dto.grade.Grade;
import cn.edu.sustech.cs307.dto.prerequisite.CoursePrerequisite;
import cn.edu.sustech.cs307.dto.prerequisite.Prerequisite;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.*;

import javax.annotation.Nullable;
import java.sql.*;
import java.sql.Date;
import java.time.DayOfWeek;
import java.util.*;

public class StudentServiceIns implements StudentService{
    @Override
    public int addStudent(int userId, int majorId, String firstName, String lastName, Date enrolledDate) {
        String sql = "insert into student values (?, ?, ?, ?);";
        PreparedStatement preparedStatement;
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();

            // add to course table not handling pre
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, userId);
            preparedStatement.setInt(2, majorId);
            preparedStatement.setString(3, firstName + " " + lastName);
            preparedStatement.setDate(4, enrolledDate);
            preparedStatement.execute();

            // close connection
            connection.close();
            preparedStatement.close();
        } catch (SQLException e) {
            throw new IntegrityViolationException();
        }
        return userId;
    }

    @Override
    public List<CourseSearchEntry> searchCourse(int studentId, int semesterId, @Nullable String searchCid, @Nullable String searchName, @Nullable String searchInstructor, @Nullable DayOfWeek searchDayOfWeek, @Nullable Short searchClassTime, @Nullable List<String> searchClassLocations, CourseType searchCourseType, boolean ignoreFull, boolean ignoreConflict, boolean ignorePassed, boolean ignoreMissingPrerequisites, int pageSize, int pageIndex) {

        return null;
    }

    @Override
    public EnrollResult enrollCourse(int studentId, int sectionId) {
        return null;
    }

    @Override
    public void dropCourse(int studentId, int sectionId) throws IllegalStateException {

    }

    @Override
    public void addEnrolledCourseWithGrade(int studentId, int sectionId, @Nullable Grade grade) {

    }

    @Override
    public void setEnrolledCourseGrade(int studentId, int sectionId, Grade grade) {

    }

    @Override
    public Map<Course, Grade> getEnrolledCoursesAndGrades(int studentId, @Nullable Integer semesterId) {
        return null;
    }

    @Override
    public CourseTable getCourseTable(int studentId, Date date) {
        return null;
    }

    @Override
    public boolean passedPrerequisitesForCourse(int studentId, String courseId) {
        return false;
    }

    @Override
    public Major getStudentMajor(int studentId) {
        String getMajor = "select *\n" +
                "from student s\n" +
                "         join major m on s.majorId = m.majorId\n" +
                "where s.userId = ?;";
        PreparedStatement preparedStatement;
        ResultSet resultSet;
        Major major = new Major();
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();

            // add to course table not handling pre
            preparedStatement = connection.prepareStatement(getMajor);
            preparedStatement.setInt(1, studentId);
            resultSet = preparedStatement.executeQuery();
            major.id = resultSet.getInt("s.majorid");
            major.name = resultSet.getString("name");
            DepartmentServiceIns departmentServiceIns = new DepartmentServiceIns();
            major.department = departmentServiceIns.getDepartment(resultSet.getInt("departmentid"));

            // close connection
            connection.close();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return major;
    }
}
