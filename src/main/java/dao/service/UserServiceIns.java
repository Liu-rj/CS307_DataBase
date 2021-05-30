package dao.service;
import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.Major;
import cn.edu.sustech.cs307.dto.User;
import cn.edu.sustech.cs307.service.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserServiceIns implements UserService{
    @Override
    public void removeUser(int userId) {
        String delCourse = "delete from users where user_id = ?;";
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();

            // execute operation
            PreparedStatement preparedStatement = connection.prepareStatement(delCourse);
            preparedStatement.setInt(1, userId);
            preparedStatement.execute();

            // close connection
            connection.close();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    // TODO: return value of getAllUsers and getUser, what to check?
    @Override
    public List<User> getAllUsers() {
        String sql = "select * from users;";
        //这个方法是将所有院系的名称(String 类型)放到一个list里面
        List<User> userList = new ArrayList<>();
        //先把所有的数据选出来，然后再一条一条放到list里面去
        ResultSet resultSet;
        User user;
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();

            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            resultSet = preparedStatement.executeQuery();


            connection.close();
            preparedStatement.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return userList;
    }

    @Override
    public User getUser(int userId) {
        return null;
    }
}
