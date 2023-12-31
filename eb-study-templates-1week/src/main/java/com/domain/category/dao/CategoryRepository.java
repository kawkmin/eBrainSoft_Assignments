package com.domain.category.dao;

import com.global.utils.PropertiesMapper;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class CategoryRepository {

  private Connection conn;
  private PreparedStatement pstmt;
  private ResultSet rs;
  private PropertiesMapper mapper;

  public CategoryRepository() {
    mapper = new PropertiesMapper();

    try {
      Class.forName("com.mysql.jdbc.Driver");
      conn = DriverManager.getConnection(mapper.getMyUrl(), mapper.getMyId(), mapper.getMyPw());

    } catch (SQLException e) {
      System.out.println("SQLException: " + e.getMessage());
      System.out.println("SQLState: " + e.getSQLState());
      System.out.println("VendorError: " + e.getErrorCode());
      throw new RuntimeException();
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public Map<Long, String> getAllCategory() {
    Map<Long, String> map = new HashMap<>();
    String sql = "SELECT category_id,category_name FROM CATEGORY";
    try {
      pstmt = conn.prepareStatement(sql);
      rs = pstmt.executeQuery();
      while (rs.next()) {
        map.put(rs.getLong("category_id"), rs.getString("category_name"));
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    return map;
  }

  public String findNameById(Long id) {
    String sql = "SELECT category_name FROM CATEGORY WHERE category_id = ?";
    try {
      pstmt = conn.prepareStatement(sql);
      pstmt.setLong(1, id);
      rs = pstmt.executeQuery();
      rs.next();
      return rs.getString(1);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }
}
