package com.loanbroker;

import com.loanbroker.handlers.HandlerThread;
import com.loanbroker.logging.Logger;
import com.loanbroker.models.BankDTO;
import com.loanbroker.models.CanonicalDTO;
import com.rabbitmq.client.*;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author Preuss
 */
public class Normalizer extends HandlerThread {

	private final Logger log = Logger.getLogger(Normalizer.class);
	private Map<String, String> queuesIn;
	private String queueOut;

	public Normalizer(Map<String, String> bankInputQueues, String queueOut) {
		this.queuesIn = bankInputQueues;
		this.queueOut = queueOut;
	}

	private CanonicalDTO convertFromBankToDto(String bankName, String message) {
		CanonicalDTO dto = null;
		String ssn = null;
		double interestRate = Double.NaN;
		if ("xml".equalsIgnoreCase(bankName)) {
			/*
			 Example
			 <LoanResponse>
			 <interestRate>4.5600000000000005</interestRate>
			 <ssn>12345678</ssn>
			 </LoanResponse>
			 */
			Document document;
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			try {
				DocumentBuilder db = dbf.newDocumentBuilder();
				document = db.parse(new ByteArrayInputStream(message.getBytes()));
				String interestRateStr = document.getElementsByTagName("interestRate").item(0).getTextContent();
				interestRate = Double.parseDouble(interestRateStr);
				ssn = document.getElementsByTagName("ssn").item(0).getTextContent();
				log.debug("Message from XML Bank parsed.");
			} catch (ParserConfigurationException e) {
				log.warning(e.getClass() + ", Message: " + e.getMessage());
				e.printStackTrace();
				if (e.getCause() != null) {
					log.warning("\t" + e.getCause().getClass() + ", " + e.getCause().getMessage());
					if (e.getCause().getCause() != null) {
						log.warning("\t\t" + e.getCause().getCause().getClass() + ", " + e.getCause().getCause().getMessage());
					}
				}
			} catch (SAXException e) {
				log.warning(e.getClass() + ", Message: " + e.getMessage());
				e.printStackTrace();
				if (e.getCause() != null) {
					log.warning("\t" + e.getCause().getClass() + ", " + e.getCause().getMessage());
					if (e.getCause().getCause() != null) {
						log.warning("\t\t" + e.getCause().getCause().getClass() + ", " + e.getCause().getCause().getMessage());
					}
				}
			} catch (IOException e) {
				log.warning(e.getClass() + ", Message: " + e.getMessage());
				e.printStackTrace();
				if (e.getCause() != null) {
					log.warning("\t" + e.getCause().getClass() + ", " + e.getCause().getMessage());
					if (e.getCause().getCause() != null) {
						log.warning("\t\t" + e.getCause().getCause().getClass() + ", " + e.getCause().getCause().getMessage());
					}
				}
			}
		} else if ("json".equalsIgnoreCase(bankName)) {
			try {
				/*
				 Example of json response:
				 {"interestRate":5.5,"ssn":1605789787}
				 */
				log.debug("From Bank JSON Message: " + message.replace("\n", "").replace(" ", "").replace("\t", ""));
				JSONObject json = new JSONObject(message);
				ssn = json.getString("ssn");
				interestRate = json.getDouble("interestRate");
			} catch (JSONException e) {
				log.debug("JSON Message (Wrong): " + message);
				log.warning(e.getClass() + ", Message: " + e.getMessage());
				e.printStackTrace();
				if (e.getCause() != null) {
					log.warning("\t" + e.getCause().getClass() + ", " + e.getCause().getMessage());
					if (e.getCause().getCause() != null) {
						log.warning("\t\t" + e.getCause().getCause().getClass() + ", " + e.getCause().getCause().getMessage());
					}
				}
				// Ignore wrong json.
			}
		} else if ("rabbitmq".equalsIgnoreCase(bankName)) {
			/*
			 example output:
			 "interestRate:5.5#ssn:160578-9787"
			 */
			log.debug("From Bank RabbitMQ Message: " + message.replace("\n", "").replace(" ", "").replace("\t", ""));
			for (String keyValue : message.split("#")) {
				String key = keyValue.split(":")[0];
				String value = keyValue.split(":")[1];
				if ("interestRate".equalsIgnoreCase(key)) {
					interestRate = Double.parseDouble(value);
				} else if ("ssn".equalsIgnoreCase(key)) {
					ssn = value;
				}
			}
		} else if ("webservice".equalsIgnoreCase(bankName)) {
			log.debug("From Bank Webservice Message: " + message.replace("\n", "").replace(" ", "").replace("\t", ""));
			CanonicalDTO messageDto = convertStringToDto(message);
			for (BankDTO bankDto : messageDto.getBanks()) {
				if ("webservice".equalsIgnoreCase(bankDto.getName())) {
					interestRate = bankDto.getInterestRate();
					ssn = messageDto.getSsn();
					break;
				}
			}
		} else {
			log.debug("From Bank UNKNOWN with name : " + bankName);
		}
		log.debug("SSN: " + ssn + ", InterestRate: " + interestRate);
		if (ssn == null || interestRate == Double.NaN) {
			dto = null;
		} else {
			if (ssn.length() == 10) {
				ssn = ssn.substring(0, 6) + "-" + ssn.substring(6);
			}
			if (ssn.length() == 11) {
				dto = new CanonicalDTO();
				dto.setSsn(ssn);
				BankDTO bank = new BankDTO(bankName, interestRate);
				log.debug("Bank: " + bank);
				dto.addBank(bank);
			} else {
				dto = null;
			}
		}
		log.debug("Message: " + dto);
		return dto;
	}

	private CanonicalDTO readBankMessage(Channel readChannel, String bankName, String queueIn) throws IOException, ConsumerCancelledException, ShutdownSignalException, InterruptedException {
		CanonicalDTO dto = null;

		try {
			GetResponse response = readChannel.basicGet(queueIn, true);
			if (response != null) {
				if (response.getBody() != null) {
					dto = convertFromBankToDto(bankName, new String(response.getBody()));
				}
			}
		} catch (IOException e) {
			log.debug("getConnection: Inside IOException");
			e.printStackTrace();
			if (e.getCause() != null && e.getMessage() == null) {
				log.debug("getConnection: Now Message is null");
				if (e.getCause() instanceof ShutdownSignalException) {
					ShutdownSignalException exception = (ShutdownSignalException) e.getCause();
					throw exception;
				}
				if (e.getCause() instanceof ConsumerCancelledException) {
					ConsumerCancelledException exception = (ConsumerCancelledException) e.getCause();
					throw exception;
				}
				if (e.getCause() instanceof InterruptedException) {
					InterruptedException exception = (InterruptedException) e.getCause();
					throw exception;
				}
			}
			throw e;
		} catch (Exception e) {
			log.debug("getConnection: Inside Exception");
			e.printStackTrace();
			if (e.getCause() != null && e.getMessage() == null) {
				log.debug("getConnection: Now Message is null");
				if (e.getCause() instanceof ShutdownSignalException) {
					ShutdownSignalException exception = (ShutdownSignalException) e.getCause();
					throw exception;
				}
				if (e.getCause() instanceof ConsumerCancelledException) {
					ConsumerCancelledException exception = (ConsumerCancelledException) e.getCause();
					throw exception;
				}
				if (e.getCause() instanceof InterruptedException) {
					InterruptedException exception = (InterruptedException) e.getCause();
					throw exception;
				}
			}

		} finally {
		}
		return dto;
	}

	private List<CanonicalDTO> readBankMessages(Map<String, Channel> readChannels) throws InterruptedException, ShutdownSignalException, ConsumerCancelledException, IOException {
		List<CanonicalDTO> messages = new ArrayList<>();

		for (Entry<String, String> keyValue : queuesIn.entrySet()) {
			String bankName = keyValue.getKey();
			String queueIn = keyValue.getValue();
			Channel readChannel = readChannels.get(queueIn);
			CanonicalDTO message = readBankMessage(readChannel, bankName, queueIn);
			if (message != null) {
				messages.add(message);
			}
		}
		return messages;
	}

	private void writeBankMessages(Channel writeChannel, List<CanonicalDTO> messages) {//throws IOException {
		try {
			for (CanonicalDTO canonicalDTO : messages) {
				String xmlStr = convertDtoToString(canonicalDTO);
				writeChannel.basicPublish("", queueOut, null, xmlStr.getBytes());
			}
		} catch (IOException e) {
			log.warning(e.getClass() + ", Message: " + e.getMessage());
			e.printStackTrace();
			if (e.getCause() != null) {
				log.warning("\t" + e.getCause().getClass() + ", " + e.getCause().getMessage());
				if (e.getCause().getCause() != null) {
					log.warning("\t\t" + e.getCause().getCause().getClass() + ", " + e.getCause().getCause().getMessage());
				}
			}
		}
	}

	@Override
	protected void doRun() {
		Connection conn = null;
		Channel writeChannel = null;
		Map<String, Channel> readChannels = new HashMap<>();
		while (!isPleaseStop()) {
			try {

				// Create Connections and channels.
				if (conn == null) {
					conn = getConnection();
				}
				if (writeChannel == null) {
					writeChannel = createChannel(conn, queueOut);
				}
				for (String queueIn : queuesIn.values()) {
					if (!readChannels.containsKey(queueIn)) {
						readChannels.put(queueIn, createChannel(conn, queueIn));
					}
				}

				try {
					while (!isPleaseStop()) {
						List<CanonicalDTO> messages = readBankMessages(readChannels);
						writeBankMessages(writeChannel, messages);
					}
				} catch (InterruptedException e) {
					log.warning(e.getClass() + ", Message: " + e.getMessage());
					e.printStackTrace();
					if (e.getCause() != null) {
						log.warning("\t" + e.getCause().getClass() + ", " + e.getCause().getMessage());
						if (e.getCause().getCause() != null) {
							log.warning("\t\t" + e.getCause().getCause().getClass() + ", " + e.getCause().getCause().getMessage());
						}
					}
				} catch (ShutdownSignalException e) {
					log.warning(e.getClass() + ", Message: " + e.getMessage());
					e.printStackTrace();
					if (e.getCause() != null) {
						log.warning("\t" + e.getCause().getClass() + ", " + e.getCause().getMessage());
						if (e.getCause().getCause() != null) {
							log.warning("\t\t" + e.getCause().getCause().getClass() + ", " + e.getCause().getCause().getMessage());
						}
					}
				} catch (ConsumerCancelledException e) {
					log.warning(e.getClass() + ", Message: " + e.getMessage());
					e.printStackTrace();
					if (e.getCause() != null) {
						log.warning("\t" + e.getCause().getClass() + ", " + e.getCause().getMessage());
						if (e.getCause().getCause() != null) {
							log.warning("\t\t" + e.getCause().getCause().getClass() + ", " + e.getCause().getCause().getMessage());
						}
					}
				} catch (IOException e) {
					log.warning(e.getClass() + ", Message: " + e.getMessage());
					e.printStackTrace();
					if (e.getCause() != null) {
						log.warning("\t" + e.getCause().getClass() + ", " + e.getCause().getMessage());
						if (e.getCause().getCause() != null) {
							log.warning("\t\t" + e.getCause().getCause().getClass() + ", " + e.getCause().getCause().getMessage());
						}
					}
				} finally {
					/*
					 for (Channel channel : readChannels.values()) {
					 closeChannel(channel);
					 channel = null;
					 }
					 closeConnection(conn);
					 conn = null;*/
				}
			} catch (IOException e) {
				log.warning(e.getClass() + ", Message: " + e.getMessage());
				e.printStackTrace();
				if (e.getCause() != null) {
					log.warning("\t" + e.getCause().getClass() + ", " + e.getCause().getMessage());
					if (e.getCause().getCause() != null) {
						log.warning("\t\t" + e.getCause().getCause().getClass() + ", " + e.getCause().getCause().getMessage());
					}
				}

				closeChannel(writeChannel);
				writeChannel = null;
				for (Channel channel : readChannels.values()) {
					closeChannel(channel);
					channel = null;
				}
				readChannels.clear();
				closeConnection(conn);
				conn = null;
			} finally {
			}
		}
	}
}
