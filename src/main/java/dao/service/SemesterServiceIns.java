package dao.service;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.Semester;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SemesterServiceIns implements SemesterService {
    @Override
    public int addSemester(String name, Date begin, Date end) {
        if (end.before(begin)) {
            throw new IllegalArgumentException();
        }
        String sql = "insert into semester values (default,?,?,?)";
        int curVal = 0;
        ResultSet resultSet;
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();

            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, name);
            preparedStatement.setDate(2, begin);
            preparedStatement.setDate(3, end);
            preparedStatement.execute();

            preparedStatement = connection.prepareStatement("select currval(pg_get_serial_sequence('semester', 'semester_id'));");
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                curVal = resultSet.getInt(1);
            }

            connection.close();
            preparedStatement.close();

            return curVal;
        } catch (SQLException e) {
            throw new IntegrityViolationException();
        }
    }

    @Override
    public void removeSemester(int semesterId) {
        String sql = "delete from semester where semester_id = ?";
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();

            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, semesterId);
            preparedStatement.execute();

            connection.close();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Semester> getAllSemesters() {
        String sql = "select * from semester;";
        //这个方法是将所有院系的名称(String 类型)放到一个list里面
        List<Semester> semesterList = new ArrayList<>();
        //先把所有的数据选出来，然后再一条一条放到list里面去
        ResultSet resultSet;
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();

            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                Semester semesterListElement = new Semester();
                semesterListElement.id = resultSet.getInt(1);
                semesterListElement.name = resultSet.getString(2);
                semesterListElement.begin = (Date) resultSet.getObject(3);
                semesterListElement.end = (Date) resultSet.getObject(4);
                semesterList.add(semesterListElement);
            }

            connection.close();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return semesterList;
    }

    @Override
    public Semester getSemester(int semesterId) {
        Semester getSemester = new Semester();
        String sql = "select * from semester where semester_id = ?;";
        ResultSet resultSet;
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();

            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            resultSet = preparedStatement.executeQuery();
            getSemester.id = resultSet.getInt(1);
            getSemester.name = resultSet.getString(2);
            getSemester.begin = resultSet.getDate(3);
            getSemester.end = resultSet.getDate(4);

            connection.close();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return getSemester;
    }
}
