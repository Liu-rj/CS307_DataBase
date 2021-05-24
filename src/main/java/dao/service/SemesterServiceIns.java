package dao.service;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.Department;
import cn.edu.sustech.cs307.dto.Semester;
import cn.edu.sustech.cs307.service.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SemesterServiceIns implements SemesterService{
    //TODO：这个class里面的抛出异常问题还没有处理

    @Override
    public int addSemester(String name, Date begin, Date end) {

        String sql = "insert into semester values (default,?,?,?)";
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();


            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, name);
            preparedStatement.setObject(2, begin);
            preparedStatement.setObject(3, end);
            preparedStatement.execute();

            connection.close();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        //TODO:这个id 具体指的是什么* @return the Semester id of new inserted line, if adding process is successful.
        //TODO：这里return的值需要处理
        return 0;
    }

    @Override
    public void removeSemester(int semesterId) {
        String sql = "delete from semester where semester.semesterId = ?";
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

        String sql = "select * from semester;";//TODO：需不需要按顺序选择（order by）
        //这个方法是将所有院系的名称(String 类型)放到一个list里面
        List<Semester> semesterList = new ArrayList<>();
        //先把所有的数据选出来，然后再一条一条放到list里面去
        ResultSet resultSet ;
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            resultSet = preparedStatement.executeQuery();

            while(resultSet.next()){
                Semester semesterListElement = new Semester();
                semesterListElement.id = resultSet.getInt(1);
                semesterListElement.name = resultSet.getString(2);
                semesterListElement.begin = (Date) resultSet.getObject(3);
                semesterListElement.end = (Date) resultSet.getObject(4);
                semesterList.add(semesterListElement);
            }

            connection.close();
            preparedStatement.close();

            return semesterList;


        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;


    }

    @Override
    public Semester getSemester(int semesterId) {

        Semester getSemester = new Semester();

        String sql = "select * from semester where semesterId = ? ;";
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();

            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.execute();

            int getSemesterId = preparedStatement.getResultSet().getInt(1);
            String getSemesterName = preparedStatement.getResultSet().getString(2);
            Date getSemesterBegin = (Date) preparedStatement.getResultSet().getObject(3);
            Date getSemesterEnd = (Date) preparedStatement.getResultSet().getObject(4);

            getSemester.id = getSemesterId;
            getSemester.name = getSemesterName;
            getSemester.begin = getSemesterBegin;
            getSemester.end = getSemesterEnd;

            connection.close();
            preparedStatement.close();

            return getSemester;


        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
        //TODO

    }
}
