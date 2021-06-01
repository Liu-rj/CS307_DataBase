package dao.service;
import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.Department;
import cn.edu.sustech.cs307.dto.Major;
import cn.edu.sustech.cs307.service.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MajorServiceIns implements MajorService{
    @Override
    public int addMajor(String name, int departmentId) {
        String add = "insert into major values (default, ?, ?)";
        int curVal = 0;
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();

            PreparedStatement preparedStatement = connection.prepareStatement(add);
            preparedStatement.setString(1, name);
            preparedStatement.setInt(2, departmentId);
            preparedStatement.execute();

            preparedStatement = connection.prepareStatement("select currval(pg_get_serial_sequence('major', 'major_id'));");
            preparedStatement.execute();
            curVal = preparedStatement.getResultSet().getInt(1);

            connection.close();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return curVal;
    }

    @Override
    public void removeMajor(int majorId) {
        String sql = "delete from major where major_id = ?";
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();

            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, majorId);
            preparedStatement.execute();

            connection.close();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Major> getAllMajors() {
        String sql = "select * from major;";
        //这个方法是将所有院系的名称(String 类型)放到一个list里面
        List<Major> majorList = new ArrayList<>();
        //先把所有的数据选出来，然后再一条一条放到list里面去
        ResultSet resultSet;
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();

            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            resultSet = preparedStatement.executeQuery();
            DepartmentServiceIns departmentServiceIns = new DepartmentServiceIns();
            // iterate every department
            while (resultSet.next()) {
                Major major = new Major();
                major.id = resultSet.getInt(1);
                major.name = resultSet.getString(2);
                major.department = departmentServiceIns.getDepartment(resultSet.getInt(3));
                majorList.add(major);
            }

            connection.close();
            preparedStatement.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return majorList;
    }

    @Override
    public Major getMajor(int majorId) {
        String sql = "select * from major where major_id = ?";
        ResultSet resultSet;
        Major major = new Major();
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();

            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, majorId);
            resultSet = preparedStatement.executeQuery();
            DepartmentServiceIns departmentServiceIns = new DepartmentServiceIns();
            major.id = resultSet.getInt("major_id");
            major.name = resultSet.getString("name");
            major.department = departmentServiceIns.getDepartment(resultSet.getInt("department_id"));

            connection.close();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return major;
    }

    @Override
    public void addMajorCompulsoryCourse(int majorId, String courseId) {
        String selCourse = "select course_id from course where id = ?;";
        String insert = "insert into course_major values (?, ?, true)";
        ResultSet resultSet;
        PreparedStatement preparedStatement;
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();

            preparedStatement = connection.prepareStatement(selCourse);
            preparedStatement.setString(1, courseId);
            resultSet = preparedStatement.executeQuery();
            int cid = resultSet.getInt(1);

            preparedStatement = connection.prepareStatement(insert);
            preparedStatement.setInt(1, cid);
            preparedStatement.setInt(2, majorId);
            preparedStatement.execute();

            connection.close();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addMajorElectiveCourse(int majorId, String courseId) {
        String selCourse = "select course_id from course where id = ?;";
        String insert = "insert into course_major values (?, ?, false)";
        ResultSet resultSet;
        PreparedStatement preparedStatement;
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();

            preparedStatement = connection.prepareStatement(selCourse);
            preparedStatement.setString(1, courseId);
            resultSet = preparedStatement.executeQuery();
            int cid = resultSet.getInt(1);

            preparedStatement = connection.prepareStatement(insert);
            preparedStatement.setInt(1, cid);
            preparedStatement.setInt(2, majorId);
            preparedStatement.execute();

            connection.close();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
