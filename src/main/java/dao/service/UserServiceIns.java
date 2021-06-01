package dao.service;
import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.Instructor;
import cn.edu.sustech.cs307.dto.Semester;
import cn.edu.sustech.cs307.dto.Student;
import cn.edu.sustech.cs307.dto.User;
import cn.edu.sustech.cs307.service.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserServiceIns implements UserService{


    @Override
    public void removeUser(int userId) {

        String sql = "delete from users where users.userId = ?";
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();

            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, userId);
            preparedStatement.execute();

            connection.close();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }



    }

    @Override
    public List<User> getAllUsers() {

        String sql = "select * from users;";//TODO：需不需要按顺序选择（order by）
        //这个方法是将所有院系的名称(String 类型)放到一个list里面
        List<User> usersList = new ArrayList<>();
        //先把所有的数据选出来，然后再一条一条放到list里面去
        ResultSet resultSet ;
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            resultSet = preparedStatement.executeQuery();

            while(resultSet.next()){
                //user是一个抽象类  student和instructor继承了它
                User usersStudnetListElement = new Student();
                User usersInstructorListElement = new Instructor();

//                semesterListElement.id = resultSet.getInt(1);
//                semesterListElement.name = resultSet.getString(2);
//                semesterListElement.begin = (Date) resultSet.getObject(3);
//                semesterListElement.end = (Date) resultSet.getObject(4);
//                semesterList.add(semesterListElement);
            }

            connection.close();
            preparedStatement.close();

            return usersList;


        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;

    }

    @Override
    public User getUser(int userId) {
        return null;
    }
}
