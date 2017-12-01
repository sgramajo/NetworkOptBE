package servlet;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;

import util.Collaborative;

/**
 * Servlet implementation class ItemData
 */
@WebServlet("/ItemData")
public class ItemData extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ItemData() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		//response.getWriter().append("Served at: ").append(request.getContextPath());
		String itemID = request.getParameter("itemID"); 
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Access-Control-Allow-Origin", "*");//cross domain request/CORS
		Gson gson = new Gson(); 
		Collaborative items = new Collaborative(); 
		String json = gson.toJson(items.retrieveOneItem(itemID));
		//response.getWriter().append("Served at: ").append(request.getContextPath());
		response.getWriter().write(json);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 * This retrieves the global baseline or collaborative Filtering (item to item)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		//doGet(request, response);
		String itemID = request.getParameter("itemID"); 
		String userID = request.getParameter("userID");
		String type = request.getParameter("type");
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Access-Control-Allow-Origin", "*");//cross domain request/CORS
		Gson gson = new Gson(); 
		Collaborative items = new Collaborative(); 
		String result = Double.toString(items.getRating(itemID, userID, type)); 
		response.getWriter().write(result);
	}

}
