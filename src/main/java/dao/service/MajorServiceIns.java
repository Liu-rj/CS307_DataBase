package dao.service;
import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.Major;
import cn.edu.sustech.cs307.service.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class MajorServiceIns implements MajorService{
    @Override
    public int addMajor(String name, int departmentId) {
        //TODO:这个return返回的是什么没有说啊，现在csw默认返回的是自增majorid
        //TODO:这里还没有写完
        String sql = "insert into department values (default,?)";
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();

            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, name);
            preparedStatement.execute();

            connection.close();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;

    }

    @Override
    public void removeMajor(int majorId) {

    }

    @Override
    public List<Major> getAllMajors() {
        return null;
    }

    @Override
    public Major getMajor(int majorId) {
        return null;
    }

    @Override
    public void addMajorCompulsoryCourse(int majorId, String courseId) {

    }

    @Override
    public void addMajorElectiveCourse(int majorId, String courseId) {

    }
}
