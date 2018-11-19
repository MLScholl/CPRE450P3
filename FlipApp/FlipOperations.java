package FlipApp;


/**
* FlipApp/FlipOperations.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from Flip.idl
* Sunday, April 22, 2018 10:22:44 PM CDT
*/

public interface FlipOperations 
{
  String current_game ();
  boolean current_banker (String account);
  void add_banker (String account);
  String join_game (String account, int secretNum);
  double check_time ();
  void start_game (String bet_amount, int duration);
  String current_gameusers ();
  String return_banker ();
  String current_bet ();
  void banker_leave ();
  void send_hash (String hash);
  String win_amount ();
  String receive_winner ();
  String get_logs ();
} // interface FlipOperations
