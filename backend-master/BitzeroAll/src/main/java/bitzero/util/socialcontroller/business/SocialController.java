package bitzero.util.socialcontroller.business;

import bitzero.util.socialcontroller.business.zm.ZM_SocialController;

public class SocialController {
     public static ISocialController GetController() {
          return new ZM_SocialController();
     }
}
