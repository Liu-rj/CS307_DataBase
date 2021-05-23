package dao.service;
import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.Department;
import cn.edu.sustech.cs307.service.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class DepartmentServiceIns implements DepartmentService{
    @Override
    public int addDepartment(String name) {
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
    public void removeDepartment(int departmentId) {

        String sql = "delete from CourseSectionClass where CourseSectionClass.id = ?";
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();

            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, departmentId);
            preparedStatement.execute();

            connection.close();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Department> getAllDepartments() {
        //TODO：这个方法是将所有院系的名称(String 类型)放到一个list里面
        //先把所有的数据选出来，然后再一条一条放到list里面去


        return null;
    }

    @Override
    public Department getDepartment(int departmentId) {
        Department getDepartment = new Department();
        //知道id，得到department的值
        //先执行命令，然后得到resultset的值
        String sql = "select * from department where departmentId = ? ;";
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();

            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.execute();
            int getDepartmentId = preparedStatement.getResultSet().getInt(1);
            String getDepartmentName = preparedStatement.getResultSet().getString(2);
            getDepartment.id = getDepartmentId;
            getDepartment.name = getDepartmentName;

            connection.close();
            preparedStatement.close();

            return getDepartment;


        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }
}
