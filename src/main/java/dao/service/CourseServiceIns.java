package dao.service;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.*;
import cn.edu.sustech.cs307.dto.prerequisite.AndPrerequisite;
import cn.edu.sustech.cs307.dto.prerequisite.CoursePrerequisite;
import cn.edu.sustech.cs307.dto.prerequisite.OrPrerequisite;
import cn.edu.sustech.cs307.dto.prerequisite.Prerequisite;
import cn.edu.sustech.cs307.service.*;
import cn.edu.sustech.cs307.exception.*;

import javax.annotation.Nullable;
import java.time.DayOfWeek;
import java.util.*;

import java.sql.*;


public class CourseServiceIns implements CourseService {
    public static void parsePre(Queue<List<Prerequisite>> presQueue, Set<List<Prerequisite>> resultSet) {
        List<Prerequisite> listTemp;
        Prerequisite preTemp = null;
        while (!presQueue.isEmpty()) {
            listTemp = presQueue.poll();
            for (Prerequisite p : listTemp) {
                if (p instanceof AndPrerequisite || p instanceof OrPrerequisite) {
                    preTemp = p;
                    listTemp.remove(p);
                    break;
                }
            }
            if (preTemp == null) {
                resultSet.add(listTemp);
                continue;
            }
            if (preTemp instanceof AndPrerequisite) {
                listTemp.addAll(((AndPrerequisite) preTemp).terms);
                presQueue.offer(listTemp);
            } else {
                for (Prerequisite p : ((OrPrerequisite) preTemp).terms) {
                    List<Prerequisite> newList = new ArrayList<>(listTemp);
                    newList.add(p);
                    presQueue.offer(newList);
                }
            }
        }
    }

    @Override
    public void addCourse(String courseId, String courseName, int credit, int classHour, Course.CourseGrading grading, @Nullable Prerequisite coursePrerequisite) {
        String sql = "insert into course values (default, ?, ?, ?, ?, ?);";
        String relation = "insert into prerequisite values (?, ?, ?);";
        PreparedStatement preparedStatement;
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();

            // add to course table not handling pre
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, courseId);
            preparedStatement.setString(2, courseName);
            preparedStatement.setInt(3, credit);
            preparedStatement.setInt(4, classHour);
            preparedStatement.setBoolean(5, grading == Course.CourseGrading.HUNDRED_MARK_SCORE);
            preparedStatement.execute();

            // return val for this interface
            preparedStatement = connection.prepareStatement("select currval(pg_get_serial_sequence('course', 'id_serial'));");
            preparedStatement.execute();
            int curVal = preparedStatement.getResultSet().getInt(1);

            // parse and add relation prerequisite
            if (coursePrerequisite != null) {
                Queue<List<Prerequisite>> presQueue = new LinkedList<>();
                Set<List<Prerequisite>> resultSet = new HashSet<>(); // result and group
                presQueue.add(new ArrayList<>());
                presQueue.peek().add(coursePrerequisite);
                parsePre(presQueue, resultSet);
                int groupNum = 1, id_serial;
                for (List<Prerequisite> l : resultSet) {
                    for (Prerequisite p : l) {
                        // query id_serial corresponding to courseId
                        preparedStatement = connection.prepareStatement("select id_serial from course where id = " + ((CoursePrerequisite) p).courseID + ";");
                        preparedStatement.execute();
                        id_serial = preparedStatement.getResultSet().getInt(1);

                        // insert record(curVal, id_serial, groupNum) into pre table
                        preparedStatement = connection.prepareStatement(relation);
                        preparedStatement.setInt(1, curVal);
                        preparedStatement.setInt(2, id_serial);
                        preparedStatement.setInt(3, groupNum);
                        preparedStatement.execute();
                        groupNum++;
                    }
                }
            }
            connection.close();
            preparedStatement.close();
        } catch (SQLException e) {
            throw new IntegrityViolationException();
        }
    }

    @Override
    public int addCourseSection(String courseId, int semesterId, String sectionName, int totalCapacity) {
        String courseSection = "insert into section values (default, ?,?,?,?);";
        String courseSemester = "insert into section_semester values (?,?);";
        PreparedStatement preparedStatement;
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();

            // query id_serial according to courseId
            preparedStatement = connection.prepareStatement("select id_serial from course where id = " + courseId + ";");
            preparedStatement.execute();
            int cid = preparedStatement.getResultSet().getInt(1);

            // insert record into courseSection table
            preparedStatement = connection.prepareStatement(courseSection);
            preparedStatement.setInt(1, cid);
            preparedStatement.setString(2, sectionName);
            preparedStatement.setInt(3, totalCapacity);
            preparedStatement.setInt(4, totalCapacity);
            preparedStatement.execute();

            // return val for this interface
            preparedStatement = connection.prepareStatement("select currval(pg_get_serial_sequence('coursesection', 'id_serial'));");
            preparedStatement.execute();
            int curId = preparedStatement.getResultSet().getInt(1);

            // insert record into section_semester
            preparedStatement = connection.prepareStatement(courseSemester);
            preparedStatement.setInt(1, curId);
            preparedStatement.setInt(2, semesterId);
            preparedStatement.execute();



            // close connection
            connection.close();
            preparedStatement.close();
            return curId;
        } catch (SQLException e) {
            throw new IntegrityViolationException();
        }
    }

    @Override
    public int addCourseSectionClass(int sectionId, int instructorId, DayOfWeek dayOfWeek, List<Short> weekList, short classStart, short classEnd, String location) {
        String classInsert = "insert into class values (default, ?, ?, ?, ?, ?, ?);";
        String classWeek = "insert into week_class values (?,?);";
        PreparedStatement preparedStatement;
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();

            // query id_serial according to given location name
            preparedStatement = connection.prepareStatement("select id_serial from location where name = " + location + ";");
            preparedStatement.execute();
            int lid = preparedStatement.getResultSet().getInt(1);

            // insert record into courseSection table
            preparedStatement = connection.prepareStatement(classInsert);
            preparedStatement.setInt(1, sectionId);
            preparedStatement.setInt(2, instructorId);
            preparedStatement.setInt(3, dayOfWeek.ordinal());
            preparedStatement.setInt(4, classStart);
            preparedStatement.setInt(5, classEnd);
            preparedStatement.setInt(6, lid);
            preparedStatement.execute();

            // return val for this interface
            preparedStatement = connection.prepareStatement("select currval(pg_get_serial_sequence('coursesection', 'id_serial'));");
            preparedStatement.execute();
            int curId = preparedStatement.getResultSet().getInt(1);

            // insert record into week_class
            preparedStatement = connection.prepareStatement(classWeek);
            for (Short week : weekList) {
                preparedStatement.setInt(1, week);
                preparedStatement.setInt(2, curId);
                preparedStatement.execute();
            }

            // close connection
            connection.close();
            preparedStatement.close();
            return curId;
        } catch (SQLException e) {
            throw new IntegrityViolationException();
        }
    }

    @Override
    public void removeCourse(String courseId) {
        //courseId represents the id of course. For example, CS307, CS309
        String delCourse = "delete from Course where Course.id = ?;";
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();

            // execute the removeCourse operation
            PreparedStatement preparedStatement = connection.prepareStatement(delCourse);
            preparedStatement.setString(1, courseId);
            preparedStatement.execute();

            // close connection
            connection.close();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void removeCourseSection(int sectionId) {
        // sectionId: id_serial in table section
        String delSection = "delete from section where id_serial = ?;";
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();

            // execute remove section operation
            PreparedStatement preparedStatement = connection.prepareStatement(delSection);
            preparedStatement.setInt(1, sectionId);
            preparedStatement.execute();

            // close connection
            connection.close();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void removeCourseSectionClass(int classId) {
        // classId: id_serial in table class
        String sql = "delete from class where id_serial = ?;";
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();

            // execute remove class operation
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, classId);
            preparedStatement.execute();

            // close connection
            connection.close();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<CourseSection> getCourseSectionsInSemester(String courseId, int semesterId) {
        String sel = "select a.section_id, a.name, a.totalCapacity, a.leftCapacity\n" +
                "from (\n" +
                "         select *\n" +
                "         from section_semester ss\n" +
                "                  join section s on ss.section_id = s.id_serial\n" +
                "     ) a\n" +
                "where a.semester_id = ?\n" +
                "  and a.courseId = ?;";
        String selCourseId = "select id_serial from course where id = ?;";
        List<CourseSection> sections = new ArrayList<>();
        PreparedStatement preparedStatement;
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();

            // execute selCourseId to get id_serial for courseId
            preparedStatement = connection.prepareStatement(selCourseId);
            preparedStatement.setString(1, courseId);
            preparedStatement.execute();
            int cid_serial = preparedStatement.getResultSet().getInt(1);

            // execute sel to get target sections
            preparedStatement = connection.prepareStatement(sel);
            preparedStatement.setInt(1, semesterId);
            preparedStatement.setInt(2, cid_serial);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                int id = resultSet.getInt(1);
                String name = resultSet.getString(2);
                int total = resultSet.getInt(3);
                int left = resultSet.getInt(4);

                CourseSection section = new CourseSection();
                section.id = id;
                section.name = name;
                section.totalCapacity = total;
                section.leftCapacity = left;

                sections.add(section);
            }

            // close connection
            connection.close();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return sections;
    }

    @Override
    public Course getCourseBySection(int sectionId) {
        Course course = new Course();
        String selCourse = "select courseId from section where id_serial = ?;";
        String courseInfo = "select * from course where id_serial = ?;";
        ResultSet resultSet;
        PreparedStatement preparedStatement;
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();

            // execute get courseId by sectionId operation
            preparedStatement = connection.prepareStatement(selCourse);
            preparedStatement.setInt(1, sectionId);
            resultSet = preparedStatement.executeQuery();
            int courseId = resultSet.getInt(1);

            // get course info according to queried courseId
            preparedStatement = connection.prepareStatement(courseInfo);
            preparedStatement.setInt(1, courseId);
            resultSet = preparedStatement.executeQuery();
            course.id = resultSet.getString("id");
            course.name = resultSet.getString("name");
            course.credit = resultSet.getInt("credit");
            course.classHour = resultSet.getInt("classHour");
            course.grading = Course.CourseGrading.values()[resultSet.getInt("grading")];

            // close connection
            connection.close();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return course;
    }

    @Override
    public List<CourseSectionClass> getCourseSectionClasses(int sectionId) {
        String selClass = "select *\n" +
                "from class c\n" +
                "         join instructor i on c.instructor = i.userId\n" +
                "         join location l on c.location = l.id_serial\n" +
                "where c.sectionId = ?;";
        String selWeek = "select week from week_class where classId = ?;";
        ResultSet resultSet, rsTemp;
        PreparedStatement preparedStatement;
        List<CourseSectionClass> classes = new ArrayList<>();
        List<Short> wkList;
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();

            // get classId by sectionId operation
            preparedStatement = connection.prepareStatement(selClass);
            preparedStatement.setInt(1, sectionId);
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                CourseSectionClass csc = new CourseSectionClass();
                csc.id = resultSet.getInt("c.id_serial");
                csc.instructor = new Instructor();
                csc.instructor.id = resultSet.getInt("instructor");
                csc.instructor.fullName = resultSet.getString("fullname");
                csc.dayOfWeek = DayOfWeek.of(resultSet.getInt("dayofweek"));
                csc.classBegin = (short) resultSet.getInt("classBegin");
                csc.classEnd = (short) resultSet.getInt("classEnd");
                csc.location = resultSet.getString("name");
                preparedStatement = connection.prepareStatement(selWeek);
                preparedStatement.setInt(1, csc.id);
                rsTemp = preparedStatement.executeQuery();
                wkList = new ArrayList<>();
                while (rsTemp.next()) {
                    wkList.add((short) rsTemp.getInt("week"));
                }
                csc.weekList = wkList;
                classes.add(csc);
            }

            // close connection
            connection.close();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return classes;
    }

    @Override
    public CourseSection getCourseSectionByClass(int classId) {
        String selSection = "select *\n" +
                "from section\n" +
                "where id_serial = (\n" +
                "    select sectionId\n" +
                "    from class\n" +
                "    where id_serial = ?);";
        ResultSet resultSet;
        PreparedStatement preparedStatement;
        CourseSection section = new CourseSection();
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();

            // get section by classId
            preparedStatement = connection.prepareStatement(selSection);
            preparedStatement.setInt(1, classId);
            resultSet = preparedStatement.executeQuery();
            section.id = resultSet.getInt("id_serial");
            section.name = resultSet.getString("name");
            section.totalCapacity = resultSet.getInt("totalcapacity");
            section.leftCapacity = resultSet.getInt("leftcapacity");

            // close connection
            connection.close();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return section;
    }

    // TODO: first finish enroll student then complete this method, may need a new table
    @Override
    public List<Student> getEnrolledStudentsInSemester(String courseId, int semesterId) {
        return null;
    }
}
