/*#undef WIFI_UDP_WORKING

#include <SPI.h>
#ifdef WIFI_UDP_WORKING
#include <WiFi.h>
#else
#include <Ethernet.h>
#endif*/

#include <Adafruit_CC3000.h>
#include <ccspi.h>
#include <SPI.h>
#include <string.h>
#include "utility/debug.h"

#include <alljoyn.h>

#include <stdint.h>
int AJ_Main(void);

//#ifdef WIFI_UDP_WORKING
//char ssid[] = "yourNetwork";     // the name of your network
//int status = WL_IDLE_STATUS;     // the Wifi radio's status
//#endif


// These are the interrupt and control pins
#define ADAFRUIT_CC3000_IRQ   CC3K_IRQ  // MUST be an interrupt pin!
// These can be any two pins
#define ADAFRUIT_CC3000_VBAT  CC3K_VBAT
#define ADAFRUIT_CC3000_CS    PIN_SPI_SS3
// Use hardware SPI for the remaining pins
// On an UNO, SCK = 13, MISO = 12, and MOSI = 11
//Adafruit_CC3000 cc3000 = Adafruit_CC3000(ADAFRUIT_CC3000_CS, ADAFRUIT_CC3000_IRQ, ADAFRUIT_CC3000_VBAT,
                                         //SPI_CLOCK_DIVIDER); // you can change this clock speed

#define WLAN_SSID       "CSU"          // cannot be longer than 32 characters!
#define WLAN_PASS       "12345abcde"
// Security can be WLAN_SEC_UNSEC, WLAN_SEC_WEP, WLAN_SEC_WPA or WLAN_SEC_WPA2
#define WLAN_SECURITY   WLAN_SEC_WPA2

#define IDLE_TIMEOUT_MS  3000      // Amount of time to wait (in milliseconds) with no data 
                                   // received before closing the connection.  If you know the server
                                   // you're accessing is quick to respond, you can reduce this value.


/**************************************************************************/
/*!
    @brief  Tries to read the IP address and other connection details
*/
/**************************************************************************/
bool displayConnectionDetails(void)
{
  uint32_t ipAddress, netmask, gateway, dhcpserv, dnsserv;
  
  if(!cc3000.getIPAddress(&ipAddress, &netmask, &gateway, &dhcpserv, &dnsserv))
  {
    Serial.println(F("Unable to retrieve the IP Address!\r\n"));
    return false;
  }
  else
  {
    Serial.print(F("\nIP Addr: ")); cc3000.printIPdotsRev(ipAddress);
    Serial.print(F("\nNetmask: ")); cc3000.printIPdotsRev(netmask);
    Serial.print(F("\nGateway: ")); cc3000.printIPdotsRev(gateway);
    Serial.print(F("\nDHCPsrv: ")); cc3000.printIPdotsRev(dhcpserv);
    Serial.print(F("\nDNSserv: ")); cc3000.printIPdotsRev(dnsserv);
    Serial.println();
    return true;
  }
}





// the setup routine runs once when you press reset:
void setup() {
  
    Serial.begin(115200);
    while (!Serial) ;


Serial.println(F("Hello, CC3000!\n")); 


  Serial.print("Free RAM: "); Serial.println(getFreeRam(), DEC);
  
  /* Initialise the module */
  Serial.println(F("\nInitializing..."));
  if (!cc3000.begin())
  {
    Serial.println(F("Couldn't begin()! Check your wiring?"));
    while(1);
  }
  
  // Optional SSID scan
  // listSSIDResults();
 
  
  Serial.print(F("\nAttempting to connect to ")); Serial.println(WLAN_SSID);
  if (!cc3000.connectToAP(WLAN_SSID, WLAN_PASS, WLAN_SECURITY)) {
    Serial.println(F("Failed!"));
    while(1);
  }
   
  Serial.println(F("Connected!"));
  
  /* Wait for DHCP to complete */
  Serial.println(F("Request DHCP"));
  while (!cc3000.checkDHCP())
  {
    delay(100); // ToDo: Insert a DHCP timeout!
  }  

  /* Display the IP address DNS, Gateway, etc. */  
  while (! displayConnectionDetails()) {
    delay(1000);
  }

 


/*#ifdef WIFI_UDP_WORKING
    // check for the presence of the shield:
    if (WiFi.status() == WL_NO_SHIELD) {
        AJ_Printf("WiFi shield not present\n");
        // don't continue:
        while (true) ;
    }

    // attempt to connect to Wifi network:
    while (wifiStatus != WL_CONNECTED) {
        Serial.print("Attempting to connect to open SSID: ");
        Serial.println(ssid);
        status = WiFi.begin(ssid);

        // wait 10 seconds for connection:
        delay(10000);

        IPAddress ip = WiFi.localIP();
        Serial.print("Connected: ");
        Serial.println(ip);
    }
#else
    byte mac[] = { 0x00, 0xAA, 0xBB, 0xCC, 0xDE, 0x02 };
    // start the Ethernet connection:
    if (Ethernet.begin(mac) == 0) {
        AJ_Printf("Failed to configure Ethernet using DHCP\n");
        // no point in carrying on, so do nothing forevermore:
        for (;;)
            ;
    }
#endif*/
}

// the loop routine runs over and over again forever:
void loop() {
    AJ_Printf("Hello\n");
    AJ_Main();
}
