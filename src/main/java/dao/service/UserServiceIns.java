package dao.service;
import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.User;
import cn.edu.sustech.cs307.service.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class UserServiceIns implements UserService{


    @Override
    public void removeUser(int userId) {
        String delCourse = "delete from users where userid = ?;";
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();

            // execute the removeCourse operation
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

    @Override
    public List<User> getAllUsers() {
        return null;
    }

    @Override
    public User getUser(int userId) {
        return null;
    }
}
