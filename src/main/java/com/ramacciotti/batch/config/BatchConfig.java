package com.ramacciotti.batch.config;

import com.ramacciotti.batch.model.Employee;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
public class BatchConfig {

    @Qualifier("dataSourceTransactionManager")
    private final PlatformTransactionManager transactionManager;

    public BatchConfig(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }


    /**
     * Job representa um processo de lote completo a ser executado.
     * Ele é composto por um ou mais "Steps" (passos),
     * Cada passo é uma unidade de trabalho menor que pode incluir tarefas como leitura, processamento e escrita de dados.
     * O Job coordena a execução desses passos e define a ordem em que eles devem ser executados.
     */
    @Bean // indica que é um método de configuração Spring
    public Job job(JobRepository jobRepository, Step step, JobListener jobListener) {
        // Cria um novo job e o associa ao repositório de Jobs fornecido.
        return new JobBuilder
                ("employeeJob", jobRepository)
                .start(step) // Define o primeiro passo (Step) a ser executado quando o Job for iniciado.
                .listener(jobListener)
                .incrementer(new RunIdIncrementer()) // Incrementa automaticamente o ID do Job para garantir IDs únicos em cada execução.
                .build(); // Constrói e retorna o objeto Job configurado.
    }


    /**
     * Cada step executa uma tarefa específica, como ler dados de uma fonte, processá-los e gravá-los em uma saída.
     */
    @Bean
    public Step step(JobRepository jobRepository, ItemReader<Employee> reader, ItemWriter<Employee> writer) {
        return new StepBuilder("saveEmployeesToDatabase", jobRepository) // O StepBuilder é usado para construir e configurar um objeto Step.
                // configura o step para processar os dados em lotes (chunks)
                .<Employee, Employee>chunk(10, transactionManager) // Isso indica que o step  processará os dados em lotes de 10 itens de uma vez.
                .reader(reader) // Aqui, estamos definindo o leitor (reader) que será usado para ler os dados de entrada
                .writer(writer) // Esta linha define o escritor (writer) que será usado para escrever os dados processados.
                .build(); // Constrói o objeto Step com todas as configurações especificadas anteriormente
    }

    @Bean
    public ItemReader<Employee> reader() {
        return new FlatFileItemReaderBuilder<Employee>()
                .name("reader") // Define um nome para o leitor
                .resource(new ClassPathResource("employees.csv")) // Especifica a localização do arquivo de origem (CSV)
                .delimited() // Configura o leitor para lidar com um arquivo delimitado (default: campos separados por vírgula)
                .names("id", "name", "title", "department", "age") // Define os nomes das colunas no arquivo
                .targetType(Employee.class) // Especifica o tipo de objeto que será lido e convertido a partir das linhas do arquivo
                .build(); // Constrói e retorna o leitor configurado
    }

    @Bean
    public ItemWriter<Employee> writer(@Qualifier("connectToDatabase") DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<Employee>()
                .dataSource(dataSource)
                .sql(
                        "INSERT INTO employee (id, name, title, department, age) values (:id, :name, :title, :department, :age)")
                .itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
                .build();
    }
}